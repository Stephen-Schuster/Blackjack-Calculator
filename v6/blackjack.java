import java.util.Queue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.nio.file.Files;
import java.nio.file.Paths;


public class blackjack {
    public static final int numDecks = 6;
    public static final double deckPen = 0.25;
    public static final boolean hitSoft17 = true;
    public static final boolean das = true;
    public static final int minDouble = 0, maxDouble = 21;
    public static final int maxSplitHands = 4;
    public static final boolean resplitAces = true;
    public static final boolean lateSurrender = true;
    public static final boolean countCards = true;
    public static final boolean deviations = true;
    public static final int minimumCount = -1, numDeckEstimationDivisions = 2;
    public static final int ace = 11;
    public static final int numRecordedCounts = 15;
    public static final int lineCharacterLength = 20;
    public static final int tableMinimum = 10;

    public static long[][] positiveOutcomes;//[trueCount][outcome]
    public static long[][] negativeOutcomes;//[trueCount][outcome]
    public static long[] blackjacks;//[trueCount]
    public static long[] total;//[trueCount]
    public static BufferedWriter rentShoeWriter, revenueShoeWriter;//stores doubles
    public static String rulesString;
    public static boolean recordShoes = false;
    public static double[] kellyBets;
    public static double minBetPerBankroll = 0.001, minimumEV = 0.2;
    public static int numShoes = 10000000;
    public static double expectedRent, expectedRevenue;
    public static double minBankroll;
    public static double kellyBetMultiplier = 1;
    public static double startingBankroll = 10000,successThresholdPerBankroll = 30, numCareers = 1000;
    public static int numUpdates = 100;

    public static void main(String[] args) throws IOException {
        initRulesString();
        if(args[0].equals("simShoes")) {
            parseArgs(args);
            loadDistributions();
            calculateKellyBets();
            initShoeWriters();
            for(int i = 0; i<numShoes; i++) {
                if(numShoes>=numUpdates && i%(numShoes/numUpdates) == 0) System.out.println((i/(numShoes/100))+"%");
                new Shoe().simulate();
            }
            rentShoeWriter.close();
            revenueShoeWriter.close();
            saveDistributions();
        }
        else if(args[0].equals("getEV")) {
            loadDistributions();
            calculateKellyBets();
            calculateEV();
            System.out.println("average rent per minimum bet per hand: "+expectedRent*tableMinimum);
            System.out.println("average return on bankroll per hand: "+expectedRevenue);
            int minBet = (int)(minBetPerBankroll*startingBankroll/tableMinimum+1)*tableMinimum;
            for(int i = 0; i<kellyBets.length; i++)System.out.println("kelly bet "+i+": "+kellyBets[i]);
            System.out.println("bankroll to make "+args[1]+" per hand: "+((startingBankroll-expectedRent*minBet)/expectedRevenue));
        }
        else if(args[0].equals("ror")) {
            numShoes = -1;
            parseArgs(args);
            loadDistributions();
            calculateKellyBets();
            calculateEV();
            RandomAccessFile rentShoes = new RandomAccessFile("rentShoes"+rulesString+".txt", "r"), 
            revenueShoes = new RandomAccessFile("revenueShoes"+rulesString+".txt", "r");
            int numRuins = 0;
            double maxBankroll = successThresholdPerBankroll*startingBankroll;
            long totalRecordedShoes = rentShoes.length()/(lineCharacterLength+1);
            for(int i = 0; i<numCareers; i++) {
                if(numCareers>=numUpdates && i%(numCareers/numUpdates) == 0) System.out.println((i/(numCareers/100))+"%"); 
                Random randomVals = new Random();
                for(int j = 0;bankroll<maxBankroll && (numShoes == -1 || j<numShoes);j++) {
                    int shoeNum = (int)(randomVals.nextDouble()*totalRecordedShoes);
                    rentShoes.seek(shoeNum*(lineCharacterLength+1));
                    revenueShoes.seek(shoeNum*(lineCharacterLength+1));
                    //System.out.println(rentShoes.readLine());
                    double rent = Double.parseDouble(rentShoes.readLine());
                    //System.out.println(revenueShoes.readLine());
                    double revenue = Double.parseDouble(revenueShoes.readLine());
                    int minBet = (int)(minBetPerBankroll*bankroll/tableMinimum+1)*tableMinimum;
                    bankroll+=Math.max(bankroll,minBankroll)*revenue+rent*minBet;
                    if(tableMinimum>bankroll) {
                        numRuins++;
                        break;
                    }
                }
            }
            rentShoes.close();
            revenueShoes.close();
            System.out.println("Risk of Ruin: "+((double)numRuins/numCareers));
        }
    }
    public static void calculateEV() {
        long totalHands = 0;
        for(int i = 0; i<total.length; i++)totalHands+=total[i];
        expectedRent = getAverage(0)*total[0]/totalHands;
        double totalRevenue = 0;
        for(int i = 1; i<numRecordedCounts; i++) {
            totalRevenue+=getAverage(i)*total[i]*kellyBets[i];
        }
        expectedRevenue = totalRevenue/totalHands;
        int minBet = (int)(minBetPerBankroll*startingBankroll/tableMinimum+1)*tableMinimum;
        System.out.println("starting EV: "+(startingBankroll*expectedRevenue+expectedRent*minBet));
        if(kellyBets != null) {
            minBankroll = Math.max((minimumEV-expectedRent*tableMinimum)/expectedRevenue, tableMinimum/kellyBets[1]);
        }
    }
    public static void saveDistributions() throws IOException {
        BufferedWriter distributionWriter = new BufferedWriter(new FileWriter(new File("countDistributions"+rulesString+".txt")));
        for(int i = 0; i<numRecordedCounts; i++) {
            String line = total[i]+","+blackjacks[i]+",";
            for(int j = negativeOutcomes[i].length-1; j>=0; j--) {
                line+=negativeOutcomes[i][j]+",";
            }
            for(int j = 0; j<positiveOutcomes[i].length; j++) {
                line+=positiveOutcomes[i][j]+",";
            }
            distributionWriter.write(line+"\n");
        }
        distributionWriter.close();
    }
    public static void loadDistributions() throws IOException {
        int numOutcomes = maxSplitHands;
        if(das) numOutcomes*=2;
        numOutcomes++;
        positiveOutcomes = new long[numRecordedCounts][numOutcomes];
        negativeOutcomes = new long[numRecordedCounts][numOutcomes];
        blackjacks = new long[numRecordedCounts];
        total = new long[numRecordedCounts];
        BufferedReader countDistributionReader = new BufferedReader(new FileReader(new File("countDistributions"+rulesString+".txt")));
        for(int i = 0; i<numRecordedCounts; i++) {
            String line = countDistributionReader.readLine();
            int commaIndex = line.indexOf(",");
            total[i] = Long.parseLong(line.substring(0,commaIndex));
            line = line.substring(commaIndex+1);
            commaIndex = line.indexOf(",");
            blackjacks[i] = Long.parseLong(line.substring(0,commaIndex));
            line = line.substring(commaIndex+1);
            for(int j = numOutcomes-1; j>=0; j--) {
                commaIndex = line.indexOf(",");
                negativeOutcomes[i][j] = Long.parseLong(line.substring(0,commaIndex));
                line = line.substring(commaIndex+1);
            }
            for(int j = 0; j<numOutcomes;j++) {
                commaIndex = line.indexOf(",");
                positiveOutcomes[i][j] = Long.parseLong(line.substring(0,commaIndex));
                line = line.substring(commaIndex+1);
            }
        }
    }
    public static void parseArgs(String[] args) {
        for(int i = 1; i<args.length; i++) {
            int equalsIndex = args[i].indexOf("=");
            if(equalsIndex == -1) {
                System.out.println("arg "+i+"does not have =!");
                continue;
            }
            String argName = args[i].substring(0,equalsIndex);
            String argValue = args[i].substring(equalsIndex+1);
            if(argName.equals("recordShoes")) recordShoes = Boolean.parseBoolean(argValue);
            else if(argName.equals("numShoes")) numShoes = Integer.parseInt(argValue);
            else if(argName.equals("startingBankroll")) startingBankroll = Double.parseDouble(argValue);
            else if(argName.equals("successThresholdPerBankroll")) successThresholdPerBankroll = Double.parseDouble(argValue);
            else if(argName.equals("numCareers")) numCareers = Integer.parseInt(argValue);
            else if(argName.equals("minimumEV")) minimumEV = Double.parseDouble(argValue);
            else if(argName.equals("minBetPerBankroll")) minBetPerBankroll = Double.parseDouble(argValue);
            else if(argName.equals("numUpdates")) numUpdates = Integer.parseInt(argValue);
            else if(argName.equals("kellyBetMultiplier")) kellyBetMultiplier = Double.parseDouble(argValue);
        }
    }
    public static void initRulesString() {
        rulesString = "";
        rulesString+=" numRecordedCounts="+numRecordedCounts;
        rulesString+=" numDecks="+numDecks;
        rulesString+=" deckPen="+deckPen;
        rulesString+=" hitSoft17="+hitSoft17;
        rulesString+=" das="+das;
        rulesString+=" double="+minDouble+"-"+maxDouble;
        rulesString+=" maxSplitHands="+maxSplitHands;
        rulesString+=" resplitAces="+resplitAces;
        rulesString+=" lateSurrender="+lateSurrender;
    }
    public static void initShoeWriters() throws IOException {
        rentShoeWriter = new BufferedWriter(new FileWriter(new File("rentShoes"+rulesString+".txt"),recordShoes));
        revenueShoeWriter = new BufferedWriter(new FileWriter(new File("revenueShoes"+rulesString+".txt"),recordShoes));
    }
    public static double getAverage(int i) {
        double sum = 0;
        sum+=1.5*blackjacks[i];
        sum-=0.5*negativeOutcomes[i][0];
        for(int j = 1; j<positiveOutcomes[i].length; j++){
            sum+=j*positiveOutcomes[i][j];
            sum-=j*negativeOutcomes[i][j];
        }
        return sum/total[i];
    }
    public static void calculateKellyBets() throws IOException {
        kellyBets = new double[numRecordedCounts];
        for(int i = 0; i<kellyBets.length; i++){
            if(total[i] == 0) {
                kellyBets = null;
                recordShoes = false;
                return;
            } 
            double average = getAverage(i);
            //get standard deviation
            double sdSum = 0;
            sdSum+=blackjacks[i]*(1.5-average)*(1.5-average);
            sdSum-=negativeOutcomes[i][0]*(0.5-average)*(0.5-average);
            for(int j = 0; j<positiveOutcomes[i].length; j++) {
                if(j>0)sdSum+=negativeOutcomes[i][j]*(-j-average)*(-j-average);
                sdSum+=positiveOutcomes[i][j]*(j-average)*(j-average);
            }
            double standardDeviation = Math.sqrt(sdSum/total[i]);
            //System.out.println(i+": "+standardDeviation);
            kellyBets[i] = average/standardDeviation;
        }
        for(int i = 0; i<kellyBets.length; i++) kellyBets[i]*=kellyBetMultiplier;
    }
}

class Shoe {
    LinkedList<Integer> shoe;
    List<Integer> unshuffledShoe;
    public int runningCount = 0;
    public Shoe() {
        if(unshuffledShoe == null) {
            unshuffledShoe = new ArrayList<Integer>();
            for(int i = 0; i<4*blackjack.numDecks; i++) {
                for(int j = 2; j<12; j++) unshuffledShoe.add(j);//2-A
                for(int j = 0; j<3; j++) unshuffledShoe.add(10);//3 face cards
            }
        }
        shoe = new LinkedList<Integer>(unshuffledShoe);
        Collections.shuffle(shoe/*,new Random(45907093)/**/);
    }
    public double insuranceEV() {
        int dollars = 0;
        for(int c : shoe) {
            if(c == 10) dollars+=2;
            else dollars--;
        }
        return (double)dollars/shoe.size();
    }
    public int trueCount() {
        return blackjack.numDeckEstimationDivisions*runningCount/((blackjack.numDeckEstimationDivisions*shoe.size()+26)/52);
    }
    public int revealCard(int c) {
        if(c>9) runningCount--;
        else if(c<7) runningCount++;
        return c;
    }
    public int drawCardFaceDown() {
        return shoe.poll();
    }
    public int drawCard() {
        return revealCard(drawCardFaceDown());
    }
    public void simulate() throws IOException {
        double revenue = 0, rent = 0;
        while(shoe.size()>blackjack.numDecks*52*blackjack.deckPen) {
            int trueCount = trueCount();
            if(blackjack.countCards && trueCount<blackjack.minimumCount) break;
            double returnOnBet = new Spot(this).simulate();
            if(!blackjack.countCards) continue;
            if(trueCount>=blackjack.numRecordedCounts) trueCount = blackjack.numRecordedCounts-1;
            //revenue/rent
            if(blackjack.recordShoes) {
                if(trueCount>0) revenue+=returnOnBet*blackjack.kellyBets[trueCount];
                else rent+=returnOnBet;
            }
            //distributions
            if(trueCount<0) trueCount = 0;
            blackjack.total[trueCount]++;
            if(returnOnBet==1.5) blackjack.blackjacks[trueCount]++;
            else if(returnOnBet<0) blackjack.negativeOutcomes[trueCount][(int)(-returnOnBet)]++;
            else blackjack.positiveOutcomes[trueCount][(int)returnOnBet]++;
        }
        if(blackjack.recordShoes)writeShoe(rent, revenue);
    }
    public static void writeShoe(double rent, double revenue) throws IOException{
        blackjack.revenueShoeWriter.write(setLength(revenue+"",blackjack.lineCharacterLength)+"\n");
        blackjack.rentShoeWriter.write(setLength(rent+"",blackjack.lineCharacterLength)+"\n");
    }
    public static String setLength(String s, int length) {
        if(s.length()>length) {
            int Eindex = s.indexOf("E");
            if(Eindex<0) return s.substring(0,length);
            else {
                return s.substring(0,Eindex-(s.length()-length))+s.substring(Eindex);
            }
        }
        while(s.length()<length) s+=" ";
        return s;
    }
}
class Hand {
    List<Integer> cards;
    Shoe shoe;
    boolean hasAce, done;
    int doubled;
    Spot spot;
    public Hand(Shoe shoe, Spot spot) {
        this.shoe = shoe;
        this.spot = spot;
        this.hasAce = false;
        this.done = false;
        this.doubled = 1;
        this.cards = new ArrayList<Integer>();
    }
    public Hand(Shoe shoe) {
        this.shoe = shoe;
        this.hasAce = false;
        this.cards = new ArrayList<Integer>();
    }
    public void addCard(int card) {
        cards.add(card);
        hasAce = hasAce || card == blackjack.ace;
    }
    public boolean isBlackJack() {
        return cards.size() == 2 && hasAce && hardTotal() == 11;
    }
    public int hardTotal() {
        int total = 0;
        for(int c : cards) {
            if(c == blackjack.ace) total++;
            else total+=c;
        }
        return total;
    }
    public boolean dealerHits() {
        int hardTotal = hardTotal();
        if(!hasAce || hardTotal>11) return hardTotal<17;
        else if(blackjack.hitSoft17) return hardTotal<=7;
        else return hardTotal<7;
    }
}
class Spot {
    List<Hand> hands;
    boolean insurance, surrender;
    Shoe shoe;
    public Spot(Shoe shoe) {
        this.hands = new ArrayList<Hand>();
        this.insurance = false;
        this.surrender = false;
        this.shoe = shoe;
        hands.add(new Hand(shoe, this));
    }
    public double simulate() {
        double toReturn = -1;
        Hand dealer = new Hand(shoe);
        dealer.addCard(shoe.drawCard());
        dealer.addCard(shoe.drawCardFaceDown());
        this.hands.get(0).addCard(shoe.drawCard());
        this.hands.get(0).addCard(shoe.drawCard());
        //insurance
        if(blackjack.countCards && blackjack.deviations && dealer.cards.get(0) == blackjack.ace && shoe.trueCount()>=3 ) {
            if(dealer.cards.get(1) == 10) {
                toReturn+=1;
            } else {
                toReturn-=0.5;
            }
        }
        //dealer blackjack
        if(dealer.isBlackJack()) {
            if(this.hands.get(0).isBlackJack()) {
                toReturn+=1;
            }
            return toReturn;
        }
        //this blackjack
        if(this.hands.get(0).isBlackJack()) {
            toReturn+=2.5;
            return toReturn;
        }
        //splits
        int handNum = 0;
        while(handNum<this.hands.size() && this.hands.size()<blackjack.maxSplitHands) {
            if(this.hands.get(handNum).cards.get(0) == this.hands.get(handNum).cards.get(1) && basicStrategy.split(this.hands.get(handNum).cards.get(0),dealer.cards.get(0), shoe)) {
                this.hands.add(new Hand(shoe,this));
                toReturn-=1;
                this.hands.get(this.hands.size()-1).addCard(this.hands.get(handNum).cards.remove(1));
                this.hands.get(handNum).addCard(shoe.drawCard());
                this.hands.get(this.hands.size()-1).addCard(shoe.drawCard());
            } else handNum++;
        }
        //surrenders
        if(this.hands.size() == 1 && basicStrategy.surrender(this.hands.get(0).hardTotal(),dealer.cards.get(0),shoe)) {
            toReturn+=0.5;
            return toReturn;
        }
        //doubles
        if(blackjack.das || this.hands.size() == 1) {
            for(int i = 0; i<this.hands.size(); i++) {
                int hardTotal = this.hands.get(i).hardTotal();
                boolean doubleDown;
                if(this.hands.get(i).hasAce && hardTotal<11){
                    doubleDown = basicStrategy.doubleSoft(hardTotal-1,dealer.cards.get(0),shoe);
                } else {
                    doubleDown = basicStrategy.doubleHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(doubleDown) {
                    this.hands.get(i).doubled = 2;
                    this.hands.get(i).addCard(shoe.drawCard());
                    this.hands.get(i).done = true;
                    toReturn-=1;
                }
            }
        }
        //hits and stands
        for(int i = 0; i<this.hands.size(); i++) {
            while(!this.hands.get(i).done) {
                int hardTotal = this.hands.get(i).hardTotal();
                if(this.hands.get(i).hasAce && hardTotal<11) {
                    this.hands.get(i).done = basicStrategy.standSoft[hardTotal-1][dealer.cards.get(0)];
                } else if(this.hands.get(i).hasAce && hardTotal==11) this.hands.get(i).done = true;
                else {
                    this.hands.get(i).done = basicStrategy.standHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(!this.hands.get(i).done) {
                    this.hands.get(i).addCard(shoe.drawCard());
                }
                this.hands.get(i).done = this.hands.get(i).done || this.hands.get(i).hardTotal()>=21;
            }
        }
        
        //dealer's turn
        shoe.revealCard(dealer.cards.get(1));
        while(dealer.dealerHits()) dealer.addCard(shoe.drawCard());
        //payout
        for(int i = 0; i<this.hands.size(); i++) {
            int hardTotal = this.hands.get(i).hardTotal();
            if(hardTotal>21) continue;
            int dealerHardTotal = dealer.hardTotal();
            if(dealerHardTotal>21) {
                toReturn+=2*this.hands.get(i).doubled;
                continue;
            }
            int bestTotal = hardTotal;
            int dealerBestTotal = dealerHardTotal;
            if(this.hands.get(i).hasAce && hardTotal<=11) bestTotal = hardTotal+10;
            if(dealer.hasAce && dealerHardTotal<=11) dealerBestTotal = dealerHardTotal+10;
            if(dealerBestTotal<bestTotal){
                toReturn+=2*this.hands.get(i).doubled;
                continue;
            }
            if(dealerBestTotal==bestTotal){
                toReturn+=1*this.hands.get(i).doubled;
                continue;
            }
        }
        return toReturn;
    }
}
class basicStrategy {
    static final boolean NA = false;
    static boolean split(int p, int d, Shoe shoe) {
        if(p == 10 && 4 <= d && d <= 6 && blackjack.countCards && blackjack.deviations) return shoe.trueCount()>=10-d;
        if(blackjack.das) {
            return splitDas[p][d];
        } else {
            return splitNoDas[p][d];
        }
    }
    static final boolean[][] splitDas = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,  true,  true,  true,  true,  true,  true, false, false, false, false},//2
        {   NA,    NA,  true,  true,  true,  true,  true,  true, false, false, false, false},//3
        {   NA,    NA, false, false, false,  true,  true, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//6
        {   NA,    NA,  true,  true,  true,  true,  true,  true, false, false, false, false},//7
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//8
        {   NA,    NA,  true,  true,  true,  true,  true, false,  true,  true, false, false},//9
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//10
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//A
    };
    static final boolean[][] splitNoDas = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA, false, false,  true,  true,  true,  true, false, false, false, false},//2
        {   NA,    NA, false, false,  true,  true,  true,  true, false, false, false, false},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false,  true,  true,  true,  true, false, false, false, false, false},//6
        {   NA,    NA,  true,  true,  true,  true,  true,  true, false, false, false, false},//7
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//8
        {   NA,    NA,  true,  true,  true,  true,  true, false,  true,  true, false, false},//9
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//10
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//A
    };
    public static boolean doubleSoft(int p, int d, Shoe shoe) {
        if(blackjack.countCards && blackjack.deviations) {
            if(p == 8) {
                if(d == 4) return shoe.trueCount()>=3;
                if(d == 5) return shoe.trueCount()>=1;
            }
            else if(p == 6 && d == 2)  return shoe.trueCount()>=1;
            else if(p == 8 && d == 6) {
                if(blackjack.hitSoft17) return shoe.runningCount>=0;
                else return shoe.runningCount>=1;
            }
        }
        if(blackjack.hitSoft17) {
            return doubleSoftH17[p][d];
        } else {
            return doubleSoftH17[p][d];
        }
    }
    static final boolean[][] doubleSoftS17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA, false, false, false,  true,  true, false, false, false, false, false},//2
        {   NA,    NA, false, false, false,  true,  true, false, false, false, false, false},//3
        {   NA,    NA, false, false,  true,  true,  true, false, false, false, false, false},//4
        {   NA,    NA, false, false,  true,  true,  true, false, false, false, false, false},//5
        {   NA,    NA, false,  true,  true,  true,  true, false, false, false, false, false},//6
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//9
    };
    static final boolean[][] doubleSoftH17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA, false, false, false,  true,  true, false, false, false, false, false},//2
        {   NA,    NA, false, false, false,  true,  true, false, false, false, false, false},//3
        {   NA,    NA, false, false,  true,  true,  true, false, false, false, false, false},//4
        {   NA,    NA, false, false,  true,  true,  true, false, false, false, false, false},//5
        {   NA,    NA, false,  true,  true,  true,  true, false, false, false, false, false},//6
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false,  true, false, false, false, false, false},//8
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//9
    };
    static final boolean[][] standSoft = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//2
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true, false, false, false},//7
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//8
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//9
    };
    public static boolean standHard(int p, int d, Shoe shoe) {
        if(blackjack.countCards && blackjack.deviations) {
            if(p == 12) {
                if(d == 2) return shoe.trueCount()>=3;
                if(d == 3) return shoe.trueCount()>=2;
                if(d == 4) return shoe.runningCount>=0;
            }
            else if(p == 13 && d == 2) return shoe.trueCount()>-1;
            else if(p == 15) {
                if(d == 10) return shoe.trueCount()>=4;
                if(d == 11 && blackjack.hitSoft17) return shoe.trueCount()>=5;
            }
            else if(p == 15) {
                if(d == 9) return shoe.trueCount()>=4;
                if(d == 10) return shoe.runningCount>0;
                if(d == 11 && blackjack.hitSoft17) return shoe.trueCount()>=3;
            }
        }
        return standHard[p][d];
    }
    static final boolean[][] standHard = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//2
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//9
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//10
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//11
        {   NA,    NA, false, false,  true,  true,  true, false, false, false, false, false},//12
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//13
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//14
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//15
        {   NA,    NA,  true,  true,  true,  true,  true, false, false, false, false, false},//16
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//17
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//18
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//19
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//20
    };
    public static boolean doubleHard(int p, int d, Shoe shoe) {
        if(blackjack.countCards && blackjack.deviations) {
            if(p == 9) {
                if(d == 2) return shoe.trueCount()>=1;
                if(d == 7) return shoe.trueCount()>=3;
            } else if(p == 8 && d == 6)   return shoe.trueCount()>=2;
            else if(p == 10) {
                if(d == 10)  return shoe.trueCount()>=4;
                if(d == blackjack.ace){
                    if(blackjack.hitSoft17) return shoe.trueCount()>=3;
                    else return shoe.trueCount()>=4;
                }
            } else if(p == 11 && d == blackjack.ace && !blackjack.hitSoft17) return shoe.trueCount()>=1;
        }
        if(blackjack.hitSoft17) {
            return doubleHardH17[p][d];
        } else {
            return doubleHardS17[p][d];
        }
    }
    static final boolean[][] doubleHardH17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//2
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false,  true,  true,  true,  true, false, false, false, false, false},//11
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true, false, false},//11
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},//11
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//12
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//13
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//14
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//15
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//16
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//17
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//18
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//19
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//20
    };
    static final boolean[][] doubleHardS17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//2
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false,  true,  true,  true,  true, false, false, false, false, false},//11
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true, false, false},//11
        {   NA,    NA,  true,  true,  true,  true,  true,  true,  true,  true,  true, false},//11
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//12
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//13
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//14
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//15
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//16
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//17
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//18
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//19
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//20
    };
    public static boolean surrender(int p, int d, Shoe shoe) {
        if(blackjack.countCards && blackjack.deviations) {
            if(p == 15) {
                if(d == 9) return shoe.trueCount()>=2;
                else if(d == 10) return shoe.trueCount()>=0;
                else if(d == blackjack.ace) {
                    if(blackjack.hitSoft17) return shoe.trueCount()>=-1;
                    else return shoe.trueCount()>=2;
                }
            } else if(p == 16) {
                if(p == 8) return shoe.trueCount()>=4;
                else if(p==9) return shoe.trueCount()>-1;
            }
        }
        if(blackjack.hitSoft17) {
            return surrenderH17[p][d];
        } else {
            return surrenderS17[p][d];
        }
    }
    static final boolean[][] surrenderH17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//2
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//9
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//10
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//11
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//12
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//13
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//14
        {   NA,    NA, false, false, false, false, false, false, false, false,  true,  true},//15
        {   NA,    NA, false, false, false, false, false, false, false,  true,  true,  true},//16
        {   NA,    NA, false, false, false, false, false, false, false, false, false,  true},//17
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//18
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//19
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//20
    };
    static final boolean[][] surrenderS17 = {
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//0
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//1
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//2
        {   NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA,    NA},//3
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//4
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//5
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//6
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//7
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//8
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//9
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//10
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//11
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//12
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//13
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//14
        {   NA,    NA, false, false, false, false, false, false, false, false,  true, false},//15
        {   NA,    NA, false, false, false, false, false, false, false,  true,  true,  true},//16
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//17
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//18
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//19
        {   NA,    NA, false, false, false, false, false, false, false, false, false, false},//20
    };
}