import java.util.Queue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;

class rorCalc {
    public static void main(String[] args) throws IOException{
        int bankroll = Integer.parseInt(args[0]);
        int numTests = Integer.parseInt(args[1]);
        int[] maxBankrollForBetspread = new int[args.length-2];
        for(int i = 0; i<maxBankrollForBetspread.length; i++) maxBankrollForBetspread[i] = Integer.parseInt(args[i+2]);
        int maxPoint = maxBankrollForBetspread[maxBankrollForBetspread.length-1];
        
        int numSuccesses = 0;
        long numShoes = 0;
        System.out.println("getting shoes");
        //System.out.println(length);
        int[][] shoeChanges = new int[maxBankrollForBetspread.length][], shoeMaxes = new int[maxBankrollForBetspread.length][], shoeMins = new int[maxBankrollForBetspread.length][];
        for(int betSpread = 0; betSpread<shoeChanges.length; betSpread++) {
            BufferedReader lengthReader = new BufferedReader(new FileReader(new File("shoes"+betSpread+".txt")));
            int length = 0;
            String line = lengthReader.readLine(); 
            for(;line != null; length++) line = lengthReader.readLine();
            shoeChanges[betSpread] = new int[length];
            shoeMaxes[betSpread] = new int[length]; 
            shoeMins[betSpread] = new int[length];
            BufferedReader reader = new BufferedReader(new FileReader(new File("shoes"+betSpread+".txt")));
            for(int i = 0; i<length;i++) {
                line = reader.readLine();
                shoeChanges[betSpread][i] = Integer.parseInt(line.substring(0, line.indexOf(",")));
                line = line.substring(line.indexOf(",")+1);
                shoeMaxes[betSpread][i] = Integer.parseInt(line.substring(0, line.indexOf(",")));
                line = line.substring(line.indexOf(",")+1);
                shoeMins[betSpread][i] = Integer.parseInt(line);
            }
        }
        System.out.println(maxPoint+" "+bankroll);
        System.out.println("starting tests");
        for(int i = 0; i<numTests;i++) {
            if(numTests>=100)if(i%(numTests/100) == 0) System.out.println(i/(numTests/100)+"%");
            int money = bankroll;
            int betSpread = 0;
            while(maxBankrollForBetspread[betSpread]<money){
                betSpread++;
                if(maxBankrollForBetspread.length-1 == betSpread) break;
            }
            for(int shoes = 0;true; shoes++) {
                int shoeNum = (int)(Math.random()*shoeChanges[betSpread].length);
                if(shoeMaxes[betSpread][shoeNum]+money>maxPoint) {
                    numSuccesses++;
                    numShoes+=shoes;
                    break;
                } else if(shoeMins[betSpread][shoeNum]+money<0) {
                    break;
                } else money+=shoeChanges[betSpread][shoeNum];
                if(betSpread < maxBankrollForBetspread.length-1) {
                    if(money>maxBankrollForBetspread[betSpread]) betSpread++;
                }
                if(betSpread>0) {
                    if(money<maxBankrollForBetspread[betSpread-1]) betSpread--;
                }
            }
            
        }
        System.out.println("success rate: "+((double)numSuccesses/numTests)+"\naverage shoes to "+(maxPoint/bankroll)+"x bankroll: "+((double)numShoes/numSuccesses));
    }
        
}

public class blackjack {
    //cmd line args, final vars are possible cmd line args I could add
    public static final int minimumBetPerBankroll = 5000, bankroll = 20000;
    public static final boolean countCards = true, deviations = true, recordShoes = true, debug = false;
    public static final int minimumCount = -1, numDeckEstimationDivisions = 2;
    public static final int numShoes = 1000000;

    //rules
    public static final int numDecks = 6;
    public static final int numOtherPlayers = 0;
    public static final double deckPen = 0.25;
    public static final boolean hitSoft17 = true;
    public static final boolean das = true;
    public static final int minDouble = 0, maxDouble = 21;
    public static final int maxSplitHands = 4;
    public static final boolean resplitAces = true;
    public static final boolean lateSurrender = true;
    public static String rulesString;

    

    public static final int ace = 11;

    //state of the game
    public static Shoe shoe;
    public static long rentMoney = 0, advantageMoney = 0;
    public static int numHands = 0;

    //count distributions
    public static final int numRecordedCounts = 15;
    public static long[] totalHands, blackjacks;
    public static long[][] negativeOutcomes, positiveOutcomes;//negativeOutcomes[x][0] is late surrenders(-0.5 bet)

    public static void main(String[] args) throws IOException{
        int maxNumBets = maxSplitHands;
        if(das) maxNumBets*=2;
        positiveOutcomes = new long[numRecordedCounts][maxNumBets+1];
        negativeOutcomes = new long[numRecordedCounts][maxNumBets+1];
        totalHands = new long[numRecordedCounts];
        blackjacks = new long[numRecordedCounts];
        rulesString = "";
        rulesString+=" numDecks="+numDecks;
        rulesString+=" deckPen="+deckPen;
        rulesString+=" hitSoft17="+hitSoft17;
        rulesString+=" das="+das;
        rulesString+=" double="+minDouble+"-"+maxDouble;
        rulesString+=" maxSplitHands="+maxSplitHands;
        rulesString+=" resplitAces="+resplitAces;
        rulesString+=" lateSurrender="+lateSurrender;
        BufferedReader countDistributionReader = new BufferedReader(new FileReader(new File("countDistributions"+rulesString+".txt")));
        for(int i = 0; i<totalHands.length;i++) {
            String line = countDistributionReader.readLine();
            int commaIndex = line.indexOf(",");
            totalHands[i] = Long.parseLong(line.substring(0,commaIndex));
            for(int j = negativeOutcomes[i].length-1; j>=0; j--) {
                line = line.substring(commaIndex+1);
                commaIndex = line.indexOf(",");
                negativeOutcomes[i][j] = Long.parseLong(line.substring(0,commaIndex));
            }
            for(int j = 0; j<positiveOutcomes[i].length; j++) {
                if(j == 2) {
                    line = line.substring(commaIndex+1);
                    commaIndex = line.indexOf(",");
                    blackjacks[i] = Long.parseLong(line.substring(0,commaIndex));
                }
                line = line.substring(commaIndex+1);
                commaIndex = line.indexOf(",");
                positiveOutcomes[i][j] = Long.parseLong(line.substring(0,commaIndex));
            }
        }
        simShoes(args);
        writeCountDistributions();
    }
    public static void writeCountDistributions() throws IOException{
        BufferedWriter countDistributionWriter = new BufferedWriter(new FileWriter(new File("countDistributions"+rulesString+".txt")));
        for(int i = 0; i<numRecordedCounts; i++) {
            countDistributionWriter.write(totalHands[i]+",");
            for(int j = negativeOutcomes[i].length-1; j>=0; j--) {
                countDistributionWriter.write(negativeOutcomes[i][j]+",");
            }
            for(int j = 0; j<positiveOutcomes[i].length; j++) {
                if(j == 2) {
                    countDistributionWriter.write(blackjacks[i]+",");
                }
                countDistributionWriter.write(positiveOutcomes[i][j]+",");
            }
            countDistributionWriter.write("\n");
        }
        countDistributionWriter.close();
    }
    public static void simShoes(String[] args) throws IOException{
        for(int i = 0; i<args.length; i++) {
            //int equalsIndex = args[i].indexOf("=");
            // if(args[i].substring(0,equalsIndex).equals("numShoes")) numShoes = Integer.parseInt(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("minimumBetPerBankroll")) minimumBetPerBankroll = Integer.parseInt(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("bankroll")) bankroll = Integer.parseInt(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("countCards")) countCards = Boolean.parseBoolean(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("recordShoes")) recordShoes = Boolean.parseBoolean(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("deviations")) deviations = Boolean.parseBoolean(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("debug")) debug = Boolean.parseBoolean(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("minimumCount")) minimumCount = Integer.parseInt(args[i].substring(equalsIndex+1));
            // else if(args[i].substring(0,equalsIndex).equals("numDeckEstimationDivisions"))numDeckEstimationDivisions  = Integer.parseInt(args[i].substring(equalsIndex+1));
        }
        BufferedWriter rentWriter = null, advantageWriter = null;
        if(recordShoes){
            rentWriter = new BufferedWriter(new FileWriter(new File("rentShoes"+rulesString+".txt")));
            advantageWriter = new BufferedWriter(new FileWriter(new File("advnatage"+rulesString+"Shoes.txt")));
        }
        for(int i = 0; i<numShoes; i++) {
            int numUpdates = 100;
            if(numShoes>=numUpdates && numShoes>100)if(i%(numShoes/numUpdates) == 0) System.out.println((i/(numShoes/100))+"%");
            Shoe shoe = new Shoe();
            while(shoe.shoe.size()>numDecks*52*deckPen && (!countCards || shoe.trueCount()>=minimumCount)) {
                simHand(shoe);
            }
            if(recordShoes) {
                rentWriter.append(rentMoney+"\n");
                advantageWriter.append(advantageMoney+"\n");
                rentMoney = advantageMoney = 0;
            }
        }
        if(recordShoes){
            rentWriter.close();
            advantageWriter.close();
        }
        //System.out.println("true1 average: "+((double)true1money/numtrue1Counts));
        //System.out.println("money made: "+(double)money+"\ndollars per shoe "+((double)money/numShoes)+"\ndollars per hand "+((double)money/numHands)+"\nhands per shoe "+((double)numHands/numShoes));
        
    }
    public static int bufferedHandCount;
    public static void simHand(Shoe shoe) {
        int moneyChange = 0;
        int trueCount = bufferedHandCount = shoe.trueCount();
        Spot player = new Spot(shoe);
        moneyChange-=2;
        Hand dealer = new Hand(shoe);
        dealer.addCard(shoe.drawCard());
        dealer.addCard(shoe.drawCardFaceDown());
        player.hands.get(0).addCard(shoe.drawCard());
        player.hands.get(0).addCard(shoe.drawCard());
        //insurance
        if(countCards && deviations && dealer.cards.get(0) == blackjack.ace && shoe.trueCount()>=3 ) {
            moneyChange--;
            //insuranceBets++;
            //expectedInsuranceValue+=shoe.insuranceEV();
            //insuranceMoney-=1;
            if(dealer.cards.get(1) == 10) {
                moneyChange+=3;
                //insuranceMoney+=3;
            }
        }
        //dealer blackjack
        if(dealer.isBlackJack()) {
            if(player.hands.get(0).isBlackJack()) {
                moneyChange+=2;
            }
            if(debug) {
                System.out.print("dealer's cards: ");
                for(int i = 0; i<dealer.cards.size(); i++) System.out.print(dealer.cards.get(i)+", ");
                System.out.println("\nmy cards: ");
                for(int j = 0; j<player.hands.get(0).cards.size(); j++) System.out.print(player.hands.get(0).cards.get(j)+", ");
                System.out.println();
            }
            return;
        }
        //player blackjack
        if(player.hands.get(0).isBlackJack()) {
            moneyChange+=5;
            if(debug) {
                System.out.print("dealer's cards: ");
                for(int i = 0; i<dealer.cards.size(); i++) System.out.print(dealer.cards.get(i)+", ");
                System.out.println("\nmy cards: ");
                for(int j = 0; j<player.hands.get(0).cards.size(); j++) System.out.print(player.hands.get(0).cards.get(j)+", ");
                System.out.println();
            }
            return;
        }
        //splits
        int handNum = 0;
        while(handNum<player.hands.size() && player.hands.size()<blackjack.maxSplitHands) {
            if(player.hands.get(handNum).cards.get(0) == player.hands.get(handNum).cards.get(1) && basicStrategy.split(player.hands.get(handNum).cards.get(0),dealer.cards.get(0), shoe)) {
                if(debug)System.out.println("split");
                player.hands.add(new Hand(shoe,player));
                moneyChange-=2;
                player.hands.get(player.hands.size()-1).addCard(player.hands.get(handNum).cards.remove(1));
                player.hands.get(handNum).addCard(shoe.drawCard());
                player.hands.get(player.hands.size()-1).addCard(shoe.drawCard());
            } else handNum++;
        }
        //surrenders
        if(player.hands.size() == 1 && basicStrategy.surrender(player.hands.get(0).hardTotal(),dealer.cards.get(0),shoe)) {
            moneyChange++;
            if(debug)System.out.println("surrender");
            if(debug) {
                System.out.print("dealer's cards: ");
                for(int i = 0; i<dealer.cards.size(); i++) System.out.print(dealer.cards.get(i)+", ");
                System.out.println("\nmy cards: ");
                for(int j = 0; j<player.hands.get(0).cards.size(); j++) System.out.print(player.hands.get(0).cards.get(j)+", ");
                System.out.println();
            }
            return;
        }
        //doubles
        if(blackjack.das || player.hands.size() == 1) {
            for(int i = 0; i<player.hands.size(); i++) {
                int hardTotal = player.hands.get(i).hardTotal();
                boolean doubleDown;
                if(player.hands.get(i).hasAce && hardTotal<11){
                    doubleDown = basicStrategy.doubleSoft(hardTotal-1,dealer.cards.get(0),shoe);
                } else {
                    doubleDown = basicStrategy.doubleHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(doubleDown) {
                    if(debug)System.out.println("double");
                    player.hands.get(i).halfBets*=2;
                    player.hands.get(i).addCard(shoe.drawCard());
                    player.hands.get(i).done = true;
                    moneyChange-=2;
                }
            }
        }
        //hits and stands
        for(int i = 0; i<player.hands.size(); i++) {
            while(!player.hands.get(i).done) {
                int hardTotal = player.hands.get(i).hardTotal();
                if(player.hands.get(i).hasAce && hardTotal<11) {
                    player.hands.get(i).done = basicStrategy.standSoft[hardTotal-1][dealer.cards.get(0)];
                } else if(player.hands.get(i).hasAce && hardTotal==11) player.hands.get(i).done = true;
                else {
                    player.hands.get(i).done = basicStrategy.standHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(!player.hands.get(i).done) {
                    player.hands.get(i).addCard(shoe.drawCard());
                    if(debug)System.out.println("hit");
                } else if(debug)System.out.println("stand");
                player.hands.get(i).done = player.hands.get(i).done || player.hands.get(i).hardTotal()>=21;
            }
        }
        
        //dealer's turn
        shoe.revealCard(dealer.cards.get(1));
        while(dealer.dealerHits()) dealer.addCard(shoe.drawCard());
        //payout
        if(debug) {
            System.out.print("dealer's cards: ");
            for(int i = 0; i<dealer.cards.size(); i++) System.out.print(dealer.cards.get(i)+", ");
            System.out.println("\nmy cards: ");
        }
        for(int i = 0; i<player.hands.size(); i++) {
            if(debug) {
                for(int j = 0; j<player.hands.get(i).cards.size(); j++) System.out.print(player.hands.get(i).cards.get(j)+", ");
                System.out.println();
            }
            int hardTotal = player.hands.get(i).hardTotal();
            if(hardTotal>21) continue;
            int dealerHardTotal = dealer.hardTotal();
            if(dealerHardTotal>21) {
                moneyChange+=(2*player.hands.get(i).halfBets);
                continue;
            }
            int bestTotal = hardTotal;
            int dealerBestTotal = dealerHardTotal;
            if(player.hands.get(i).hasAce && hardTotal<=11) bestTotal = hardTotal+10;
            if(dealer.hasAce && dealerHardTotal<=11) dealerBestTotal = dealerHardTotal+10;
            if(dealerBestTotal<bestTotal){
                moneyChange+=(2*player.hands.get(i).halfBets);
                continue;
            }
            if(dealerBestTotal==bestTotal){
                moneyChange+=(player.hands.get(i).halfBets);
                continue;
            }
        }
        if(0<=bufferedHandCount && bufferedHandCount < numRecordedCounts) {
            if(moneyChange%2 != 0) {
                if(moneyChange>0) blackjacks[bufferedHandCount]++;
                else negativeOutcomes[bufferedHandCount][0]++;
            } else if(moneyChange<0) {
                negativeOutcomes[bufferedHandCount][-moneyChange/2]++;
            } else {
                positiveOutcomes[bufferedHandCount][moneyChange/2]++;
            }
            totalHands[bufferedHandCount]++;
        }
        if(bufferedHandCount>0) {
            advantageMoney+=moneyChange/2;
        } else {
            rentMoney+=moneyChange/2;
        }
        numHands++;
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
}
class Hand {
    List<Integer> cards;
    int halfBets;
    Shoe shoe;
    boolean hasAce, done;
    Spot spot;
    public Hand(Shoe shoe, Spot spot) {
        this.shoe = shoe;
        this.halfBets = 2;
        this.spot = spot;
        this.hasAce = false;
        this.done = false;
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