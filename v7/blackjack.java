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
import java.util.Date;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.nio.file.Files;
import java.nio.file.Paths;


public class blackjack {
    //things that should maybe be parameters
    //casino rules/environment
    public static final int numDecks = 6;
    public static final double deckPen = 0.25;
    public static final boolean hitSoft17 = true;
    public static final boolean das = true;
    public static final int minDouble = 0, maxDouble = 21;
    public static final int maxSplitHands = 4;
    public static final boolean resplitAces = true;
    public static final boolean lateSurrender = true;
    public static int tableMinimum = 10, tableMaximum = 1000;
    public static final double averageOtherPlayers=1.5, otherPlayersSTD=0.8;
    //player play
    public static final boolean countCards = true;
    public static final boolean deviations = true;//maybe pick deviations?
    public static final int minimumCount = -1, numDeckEstimationDivisions = 2, betRounding = 5, maxOtherPlayers=4;
    public static double susChance = 0.001, susRatio = 30; //susChance is the ideal chance of using a betspread with susRatio(Note: per hand, not per shoe; hand counts are not independent events)
    public static double minimumEV = 0.2;//how much is your time worth?
    public static int susCount = -1;
    public static double kellyBetMultiplier = -1;
    //simulation settings
    public static int numShoes = -1, numCareers = 1000;
    public static double startingBankroll = 10000,successThresholdPerBankroll = 10;
    public static int numUpdates = 100;//see update method
    public static int numProfitDivisions = 5;//how many sections should be used for "Profit per hand with Bankroll X-Y: (Profit)"

    //constants/calculated values(not paramaters)
    public static final int ace = 11;
    //distribution stuff
    public static final int numRecordedCounts = 15;//higher counts means it takes longer to get the accurate distributions, but you can kelly bet accurately at those higher counts
    public static long[][] positiveOutcomes;//[trueCount][outcome]
    public static long[][] negativeOutcomes;//[trueCount][outcome] negativeOutcomes[trueCount][0] is for surrenders
    public static long[] blackjacks;//[trueCount]
    public static long[] total;//[trueCount]
    public static double[] evPerBet;;//[trueCount]
    public static String rulesString;//a string that has all the rules, used in the name of the distributions file
    public static double[] kellyBets;
    public static double minBetPerBankroll;//makes betspread smaller for less suspicion
    public static double minBankroll;//below this bankroll, pretend we have this bankroll bc otherwise the EV wouldnt be worth our time
    public static double[] profitByBankroll;//used for "Profit per hand with Bankroll X-Y: (Profit)"
    public static long[] handsByBankroll;//used for "Profit per hand with Bankroll X-Y: (Profit)"


    public static String addCommas(int num) {
        if(num <= 0) return num+"";
        String toReturn = "";
        while(num > 0) {
            toReturn = (num%1000)+","+toReturn;
            num /= 1000;
        }
        return toReturn.substring(0,toReturn.length()-1);
    }

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        initRulesString();
        loadDistributions();
        calculateEVs();
        initLogger(args);
        if(args[0].equals("simCareers")) {
            long totalHands = 0, totalShoes= 0, totalFailureHands = 0;
            profitByBankroll = new double[numProfitDivisions];
            handsByBankroll = new long[numProfitDivisions];
            calculateKellyBets();
            calculateMinBankroll();
            int numRuins = 0;
            double maxBankroll = successThresholdPerBankroll*startingBankroll;
            for(int i = 0; i<numCareers; i++) {
                update(i, numCareers);
                int careerHands = 0;
                double bankroll = startingBankroll;
                for(int j = 0;bankroll<maxBankroll && (numShoes == -1 || j<numShoes);j++) {
                    Shoe shoe = new Shoe(Math.max(minBankroll,bankroll));
                    shoe.simulate();
                    profitByBankroll[(int)(numProfitDivisions*bankroll/maxBankroll)]+=shoe.money;
                    handsByBankroll[(int)(numProfitDivisions*bankroll/maxBankroll)]+=shoe.numHands;
                    totalHands+=shoe.numHands;
                    careerHands+=shoe.numHands;
                    totalShoes++;
                    if(bankroll+shoe.lowestPoint<=0) {//if bankrupt
                        numRuins++;
                        totalFailureHands+=careerHands;
                        break;
                    }
                    bankroll+=shoe.money;
                }
            }
            for(int i = 0; i<numProfitDivisions; i++) {
                String toLog = "Profit per hand with bankroll of $"+addCommas((int)(maxBankroll*i/numProfitDivisions))+"-$"+(int)(maxBankroll*(i+1)/numProfitDivisions/1000)+": "+(profitByBankroll[i]/handsByBankroll[i]);
                logln(toLog);
            }
            logln("Risk of ruin: "+((double)numRuins/numCareers));
            logln("Hands per Shoe: "+((double)totalHands/totalShoes));
            int avgHandsToSucceed = (int)((double)(totalHands-totalFailureHands)/(numCareers-numRuins));
            logln("Average hands to succeed: "+addCommas(avgHandsToSucceed));
        } else if(args[0].equals("simShoes")) {//basically calulate distributions
            for(int i = 0; i<numShoes; i++) {
                update(i, numShoes);
                new Shoe().simulate();
            }
        } else if(args[0].equals("getBets")) {
            calculateKellyBets();
            calculateMinBankroll();
            double effectiveBankroll = Math.max(minBankroll,startingBankroll);
            logln("Minimum bet: "+Math.max(tableMinimum,effectiveBankroll*minBetPerBankroll));
            for(int i = 0; i<numRecordedCounts; i++)logln("bet @ true "+i+": "+getBet(effectiveBankroll, i));
            System.out.println("EV per hand: "+getEV(effectiveBankroll));
        }
        saveDistributions();
        closeLogger();
    }
    
    public static BufferedWriter logger;
    public static void initLogger(String[] args) throws IOException{
        String logFileName = "";
        for(int i = 0; i<args.length; i++) logFileName+=args[i]+" ";
        logger = new BufferedWriter(new FileWriter(new File("logs/"+logFileName+".txt"), true));
        logger.write("\nProgram start at "+new Date().toString()+"\n");
        System.out.println("Program start at "+new Date().toString());
    }

    public static void logln(String s)throws IOException {
        logger.write(s+"\n");
        System.out.println(s);
    }

    public static void closeLogger() throws IOException{
        logger.write("Program end at "+new Date().toString()+"\n");
        logger.close();
    }
    public static void update(int i, int total) {
        if(total>=numUpdates && i%(total/numUpdates) == 0) System.out.println((i/(total/100.0))+"%");
    }
    public static double getBet(double effectiveBankroll, int bettingTrueCount) {
        double minBet = Math.max(tableMinimum,effectiveBankroll*minBetPerBankroll);
        double bet = effectiveBankroll*kellyBets[bettingTrueCount];
        return Math.round(Math.max(minBet, bet)/betRounding)*betRounding;
    }
    public static void calculateEVs() {
        //calculates the expected return on a hand's initial bet at each count
        evPerBet = new double[numRecordedCounts];
        for(int i = 0; i<numRecordedCounts; i++) {
            if(total[i] == 0) continue;
            double sum = 0;
            sum+=1.5*blackjacks[i];
            sum-=0.5*negativeOutcomes[i][0];
            for(int j = 1; j<positiveOutcomes[i].length; j++){
                sum+=j*positiveOutcomes[i][j];
                sum-=j*negativeOutcomes[i][j];
            }
            evPerBet[i] = sum/total[i];
            //logln("EV per bet with true count of "+i+": "+evPerBet[i]);
        }
    }
    public static void calculateMinBankroll() {
        int tableMinCount = 1;//represents the lowest count where if we bet table minimum at this count, the ev is below minimumEV
        double currBankroll = tableMinimum/kellyBets[tableMinCount];//what would our bankroll be if we bet the table minimum at the given count
        double ev = getEV(currBankroll);
        while(ev>minimumEV && tableMinCount<numRecordedCounts) {
            currBankroll = tableMinimum/kellyBets[tableMinCount];
            ev = getEV(currBankroll);
            tableMinCount++;//increment the count at which we bet table minimum until we are making below minimumEV
        }
        double rent = 0;//when betting table minimum
        long totalHands = 0;
        //calculate EV when betting table minimum
        for(int i = 0; i<tableMinCount; i++) {
            rent+=evPerBet[i]*tableMinimum*total[i];
            totalHands+=total[i];
        }
        //calculate revenue to bankroll ratio when the count is at or above the table minimum count calculated earlier
        double revenuePerBankroll = 0;
        for(int i = tableMinCount; i<numRecordedCounts; i++) {
            revenuePerBankroll+=evPerBet[i]*kellyBets[i]*total[i];
            totalHands+=total[i];
        }
        minBankroll = (minimumEV-rent/totalHands)/(revenuePerBankroll/totalHands);
        //minBetPerBankroll
        if(susCount<0) {//what is the lowest minimum bet we can make to keep our suspicion at the desired level
            int susTotal = (int)(susChance*totalHands);
            susCount = numRecordedCounts-1;
            for(int i = 1; i<numRecordedCounts; i++) {
                if(total[i]<susTotal) {
                    susCount = i;
                    break;
                }
            }
        }
        minBetPerBankroll = kellyBets[susCount]/susRatio;

    }
    //used to get the theorhetical ev for a given bankroll
    public static double getEV(double effectiveBankroll) {
        double totalEV = 0;
        long totalHands = 0;
        for(int i = 0; i<numRecordedCounts; i++) {
            totalEV+=evPerBet[i]*getBet(effectiveBankroll, i)*total[i];
            totalHands+=total[i];
        }
        return totalEV/totalHands;
    }
    public static void saveDistributions() throws IOException {
        //format: total hands, number of blackjacks, -8 bets return on inital bet, -7 bets return on inital bet, -6...-1 bets return on inital bet, surrenders, pushes, +1 bets return on inital bet, +8... bets return on inital bet,
        //the 8 is because you can split into 4 hands and double all of them, the 8 value may vary with rules
        //1 line per recorded count, starting with 0
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
        //format: total hands, number of blackjacks, -8 bets return on inital bet, -7 bets return on inital bet, -6...-1 bets return on inital bet, surrenders, pushes, +1 bets return on inital bet, +8... bets return on inital bet,
        //the 8 is because you can split into 4 hands and double all of them, the 8 value may vary with rules
        //1 line per recorded count, starting with 0
        int numOutcomes = maxSplitHands;
        if(das) numOutcomes*=2;
        numOutcomes++;
        positiveOutcomes = new long[numRecordedCounts][numOutcomes];
        negativeOutcomes = new long[numRecordedCounts][numOutcomes];
        blackjacks = new long[numRecordedCounts];
        total = new long[numRecordedCounts];
        File cdFile = new File("countDistributions"+rulesString+".txt");
        if(!cdFile.exists()) return;
        BufferedReader countDistributionReader = new BufferedReader(new FileReader(cdFile));
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
        countDistributionReader.close();
    }
    public static void parseArgs(String[] args) throws IllegalArgumentException {
        for(int i = 1; i<args.length; i++) {
            int equalsIndex = args[i].indexOf("=");
            if(equalsIndex == -1) {
                throw new IllegalArgumentException("arg "+i+"does not have =");
            }
            String argName = args[i].substring(0,equalsIndex);
            String argValue = args[i].substring(equalsIndex+1);
            if(argName.equals("numShoes")) numShoes = Integer.parseInt(argValue);
            else if(argName.equals("bankroll")) startingBankroll = Double.parseDouble(argValue);
            else if(argName.equals("successThresholdPerBankroll")) successThresholdPerBankroll = Double.parseDouble(argValue);
            else if(argName.equals("numCareers")) numCareers = Integer.parseInt(argValue);
            else if(argName.equals("minimumEV")) minimumEV = Double.parseDouble(argValue);
            else if(argName.equals("minBetPerBankroll")) minBetPerBankroll = Double.parseDouble(argValue);
            else if(argName.equals("numUpdates")) numUpdates = Integer.parseInt(argValue);
            else if(argName.equals("kellyBetMultiplier")) kellyBetMultiplier = Double.parseDouble(argValue);
            else if(argName.equals("numProfitDivisions")) numProfitDivisions = Integer.parseInt(argValue);
            else if(argName.equals("tableMinimum")) tableMinimum = Integer.parseInt(argValue);
            else if(argName.equals("tableMaximum")) tableMaximum = Integer.parseInt(argValue);
            else throw new IllegalArgumentException(argName +" is not a valid arg!");
        }
    }
    public static void calculateKellyBets() throws Exception {
        kellyBets = new double[numRecordedCounts];
        //calculate ideal kelly bet multiplier, which should be the percentage of positive counts
        if(kellyBetMultiplier == -1) {
            long totalHands = 0;
            for(int i = 0; i<numRecordedCounts; i++) totalHands+=total[i];
            kellyBetMultiplier = 1-(double)total[0]/totalHands;
        }
        for(int i = 1; i<kellyBets.length; i++){
            if(total[i] == 0) {
                kellyBets = null;
                throw new Exception("distribtions not calculated");
            } 
            double average = evPerBet[i];
            //get standard deviation
            double sdSum = 0;
            sdSum+=blackjacks[i]*(1.5-average)*(1.5-average);
            sdSum-=negativeOutcomes[i][0]*(0.5-average)*(0.5-average);
            for(int j = 0; j<positiveOutcomes[i].length; j++) {
                if(j>0)sdSum+=negativeOutcomes[i][j]*(-j-average)*(-j-average);
                sdSum+=positiveOutcomes[i][j]*(j-average)*(j-average);
            }
            double standardDeviation = Math.sqrt(sdSum/total[i]);
            kellyBets[i] = average/standardDeviation*kellyBetMultiplier;
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
}

class Shoe {
    //the Integer represents the cards value, 10 for face cards, 11 for A
    LinkedList<Integer> shoe;
    List<Integer> unshuffledShoe;
    public int runningCount = 0;
    double money = 0, lowestPoint = 0;
    double effectiveBankroll;
    int numOtherPlayers;
    int numHands;
    //standard constructor used for simulating careers
    public Shoe(double effectiveBankroll) {
        if(unshuffledShoe == null) {//if we dont have an unshuffled shoe yet, make one
            unshuffledShoe = new ArrayList<Integer>();
            for(int i = 0; i<4*blackjack.numDecks; i++) {
                for(int j = 2; j<12; j++) unshuffledShoe.add(j);//2-10 and A
                for(int j = 0; j<3; j++) unshuffledShoe.add(10);//3 face cards
            }
        }
        //copy the shoe and shuffle it
        shoe = new LinkedList<Integer>(unshuffledShoe);
        Collections.shuffle(shoe/*,new Random(45907093)/**/);//new Random(45907093) used for testing
        //get a random number of other players according to the other players distribution
        double r1 = Math.random()*(blackjack.maxOtherPlayers+1)-0.5;
        double r2 = Math.random();
        while(r2>Math.exp(-0.5*Math.pow((r1-blackjack.averageOtherPlayers)/blackjack.otherPlayersSTD,2))) {
            r1 = Math.random()*(blackjack.maxOtherPlayers+1)-0.5;
            r2 = Math.random();
        }
        numOtherPlayers = (int)(r1+0.5);
        this.effectiveBankroll = effectiveBankroll;
    }
    //this constructor is for simulating shoes for calculating the distribution
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
        numOtherPlayers = 0;
        this.effectiveBankroll = 0;
    }
    public int trueCount() {
        if((blackjack.numDeckEstimationDivisions*shoe.size()+26)/52 == 0) return blackjack.numDeckEstimationDivisions*runningCount;
        return blackjack.numDeckEstimationDivisions*runningCount/((blackjack.numDeckEstimationDivisions*shoe.size()+26)/52);
    }
    public int revealCard(int c) {//count the card
        if(c>9) runningCount--;
        else if(c<7) runningCount++;
        return c;
    }
    public int drawCardFaceDown() {//draw card but dont reveal so dont add it to the count
        return shoe.poll();
    }
    public int drawCard() {//draw card face up, so draw card face down and immediately reveal it
        return revealCard(drawCardFaceDown());
    }
    public void simulate() throws IOException {
        while(shoe.size()>blackjack.numDecks*52*blackjack.deckPen) {//go until the cut card
            int trueCount = trueCount();
            if(blackjack.countCards && trueCount<blackjack.minimumCount) break;//leave the table if count is too low
            numHands++;
            //clamp true count to 0-(numRecordedCounts-1) for betting
            if(trueCount<0) trueCount = 0;
            if(trueCount>=blackjack.numRecordedCounts) trueCount = blackjack.numRecordedCounts-1;
            double bet = 0;
            //let bet be 0 if we're calculating distributions
            if(blackjack.kellyBets != null)bet = blackjack.getBet(effectiveBankroll,trueCount);
            //number of spots should increase if the desired bet>table maximum or if near the end of the shoe(effectively increase penetration)
            //but it cant be more than the number of available spots and you cant split your desired but such that it would be less than the table minimum
            int numSpots = Math.max(Math.min(2,Math.min((int)(bet/blackjack.tableMinimum),7-numOtherPlayers)),1);
            bet=Math.min(bet/numSpots,blackjack.tableMaximum);
            int betsWagered = 0;//number of times you bet your bet, including multiple spots, splits and doubles
            Hand dealer = new Hand(this);
            double totalReturnOnBet = 0;
            for(int i = 0; i<numSpots; i++) {
                Spot spot = new Spot(this,dealer);//simulate all my spots
                double returnOnBet = spot.simulate();
                totalReturnOnBet+=returnOnBet;
                betsWagered+=spot.getWager();
                //update distributions based on this single spot
                if(!blackjack.countCards) continue;
                blackjack.total[trueCount]++;
                if(returnOnBet==1.5) blackjack.blackjacks[trueCount]++;
                else if(returnOnBet<0) blackjack.negativeOutcomes[trueCount][(int)(-returnOnBet)]++;
                else blackjack.positiveOutcomes[trueCount][(int)returnOnBet]++;
            }
            revealCard(dealer.cards.get(1));
            lowestPoint = Math.min(lowestPoint, money-betsWagered*bet);//first you have to put all the money on the table
            money+=totalReturnOnBet*bet;//then you add the money gained or lost
            for(int i = 0; i<numOtherPlayers; i++) new Spot(this,dealer).simulate();//simulate other players
        }
    }
}
class Hand {
    //the Integer represents the cards value, 10 for face cards, 11 for A
    List<Integer> cards;
    Shoe shoe;
    boolean hasAce, done;
    int doubled;//1 if not doubled, 2 if doubled
    Spot spot;
    boolean playedAsDealer = false;//makes sure the dealer only plays once bc this is called by every spot
    public Hand(Shoe shoe, Spot spot) {//standard player hand constructor
        this.shoe = shoe;
        this.spot = spot;
        this.hasAce = false;
        this.done = false;
        this.doubled = 1;
        this.cards = new ArrayList<Integer>();
    }
    public Hand(Shoe shoe) {//dealer hand constructor
        this.shoe = shoe;
        this.hasAce = false;
        this.cards = new ArrayList<Integer>();
        addCard(shoe.drawCard());
        addCard(shoe.drawCardFaceDown());
    }
    public void addCard(int card) {//hit
        cards.add(card);
        hasAce = hasAce || card == blackjack.ace;
    }
    public boolean isBlackJack() {
        return cards.size() == 2 && hasAce && hardTotal() == 11;
    }
    public int hardTotal() {//treat ace as 1
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
    public void playAsDealer() {
        if(playedAsDealer) return;
        playedAsDealer = true;
        while(dealerHits()) addCard(shoe.drawCard());
    }
}
class Spot {
    List<Hand> hands;//if hands.size()>1 it means the hands were split
    boolean insurance, surrender;//bets that change how the spot can be played
    Shoe shoe;
    Hand dealer;
    public Spot(Shoe shoe, Hand dealer) {
        this.hands = new ArrayList<Hand>();
        this.insurance = false;
        this.surrender = false;
        this.shoe = shoe;
        this.dealer = dealer;
        hands.add(new Hand(shoe, this));
    }
    public int getWager() {
        int wager = 0;
        for(Hand h : hands) {
            wager+=h.doubled;
        }
        return wager;
    }
    public double simulate() {
        double toReturn = -1;
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
        while(handNum<this.hands.size() && this.hands.size()<blackjack.maxSplitHands && (blackjack.resplitAces || hands.size() == 1)) {
            if(this.hands.get(handNum).cards.get(0) == this.hands.get(handNum).cards.get(1) && basicStrategy.split(this.hands.get(handNum).cards.get(0),dealer.cards.get(0), shoe)) {
                this.hands.add(new Hand(shoe,this));
                toReturn-=1;
                this.hands.get(this.hands.size()-1).addCard(this.hands.get(handNum).cards.remove(1));//move second card of this hand to the new hand
                //draw a new card to this hand and the new hand
                this.hands.get(handNum).addCard(shoe.drawCard());
                this.hands.get(this.hands.size()-1).addCard(shoe.drawCard());
                //stop if its aces
                this.hands.get(handNum).done = true;
                this.hands.get(this.hands.size()-1).done = true;
            } else handNum++;//only go to the next hand if we cant split this one
        }
        //surrenders
        //this is done after splits bc basic strategy checks for splits before surrenders so we have to make sure we didnt split first
        if(this.hands.size() == 1 && basicStrategy.surrender(this.hands.get(0).hardTotal(),dealer.cards.get(0),shoe)) {
            toReturn+=0.5;
            return toReturn;
        }
        //doubles
        if(blackjack.das || this.hands.size() == 1) {//make sure we can double
            for(int i = 0; i<this.hands.size(); i++) {
                int hardTotal = this.hands.get(i).hardTotal();
                boolean doubleDown;
                if(this.hands.get(i).hasAce && hardTotal<11){//check double soft strategy
                    doubleDown = basicStrategy.doubleSoft(hardTotal-1,dealer.cards.get(0),shoe);
                } else {//check double hard strategy
                    doubleDown = basicStrategy.doubleHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(doubleDown) {
                    this.hands.get(i).doubled = 2;
                    this.hands.get(i).addCard(shoe.drawCard());
                    this.hands.get(i).done = true;//cant draw after double
                    toReturn-=1;
                }
            }
        }
        //hits and stands
        for(int i = 0; i<this.hands.size(); i++) {
            while(!this.hands.get(i).done) {
                int hardTotal = this.hands.get(i).hardTotal();
                if(this.hands.get(i).hasAce && hardTotal<11) {//stop if basic strategy stand soft says so
                    this.hands.get(i).done = basicStrategy.standSoft[hardTotal-1][dealer.cards.get(0)];
                } else if(this.hands.get(i).hasAce && hardTotal==11) this.hands.get(i).done = true;//stop if ur at 21 with Ace valued at 11
                else {//stop if basic strategy stand hard says so
                    this.hands.get(i).done = basicStrategy.standHard(hardTotal,dealer.cards.get(0),shoe);
                }
                if(!this.hands.get(i).done) {//hit if you didnt stand
                    this.hands.get(i).addCard(shoe.drawCard());
                }
                this.hands.get(i).done = this.hands.get(i).done || this.hands.get(i).hardTotal()>=21;//stop if you get 21 or bust
            }
        }
        
        //dealer's turn
        dealer.playAsDealer();
        //payout
        for(int i = 0; i<this.hands.size(); i++) {
            int hardTotal = this.hands.get(i).hardTotal();
            if(hardTotal>21) continue;//you busted
            int dealerHardTotal = dealer.hardTotal();
            if(dealerHardTotal>21) {//dealer busted
                toReturn+=2*this.hands.get(i).doubled;
                continue;
            }
            //get the best total of player and dealer, Ace valued at 11 unless thats over 21, in which case you value Ace at 1(hard total)
            int bestTotal = hardTotal;
            int dealerBestTotal = dealerHardTotal;
            if(this.hands.get(i).hasAce && hardTotal<=11) bestTotal = hardTotal+10;
            if(dealer.hasAce && dealerHardTotal<=11) dealerBestTotal = dealerHardTotal+10;
            if(dealerBestTotal<bestTotal){//win
                toReturn+=2*this.hands.get(i).doubled;
                continue;
            }
            if(dealerBestTotal==bestTotal){//push
                toReturn+=1*this.hands.get(i).doubled;
                continue;
            }
        }
        return toReturn;
    }
}
class basicStrategy {
    static final boolean NA = false;//NA means not applicable aka this is impossible
    //figure out split decision based on rules and whether or not we're simulating card count with deviations or not
    static boolean split(int p, int d, Shoe shoe) {
        if(p == 10 && 4 <= d && d <= 6 && blackjack.countCards && blackjack.deviations) return shoe.trueCount()>=10-d;
        if(blackjack.das) {
            return splitDas[p][d];
        } else {
            return splitNoDas[p][d];
        }
    }
    static final boolean[][] splitDas = {//[split card][dealer upcard]
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
    static final boolean[][] splitNoDas = {//[split card][dealer upcard]
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
    //figure out double soft decision based on rules and whether or not we're simulating card count with deviations or not
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
    static final boolean[][] doubleSoftS17 = {//[non ace card][dealer upcard]
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
    static final boolean[][] doubleSoftH17 = {//[non ace card][dealer upcard]
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
    static final boolean[][] standSoft = {//[non ace card][dealer upcard]
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
    //figure out stand hard decision based on rules and whether or not we're simulating card count with deviations or not
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
    static final boolean[][] standHard = {//[hard total][dealer upcard]
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
    //figure out double hard decision based on rules and whether or not we're simulating card count with deviations or not
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
    static final boolean[][] doubleHardH17 = {//[hard total][dealer upcard]
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
    static final boolean[][] doubleHardS17 = {//[hard total][dealer upcard]
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
    //figure out surrender decision based on rules and whether or not we're simulating card count with deviations or not
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
    static final boolean[][] surrenderH17 = {//[hard total][dealer upcard]
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
    static final boolean[][] surrenderS17 = {//[hard total][dealer upcard]
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