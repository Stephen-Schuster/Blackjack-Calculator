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
    public static final int numDecks = 6;
    public static final double deckPen = 0.25;
    public static final boolean hitSoft17 = true;
    public static final boolean das = true;
    public static final int minDouble = 0, maxDouble = 21;
    public static final int maxSplitHands = 4;
    public static final boolean resplitAces = true;
    public static final boolean lateSurrender = true;
    public static final int gradualBetChange = 25;
    public static int[][] betSpread = {{1,10},{1,15},{1,200}};
    public static int betSpreadNum = 0;
    public static boolean countCards = false, deviations = true;

    public static final int minimumCount = -1, numDeckEstimationDivisions = 2;

    public static final int ace = 11, hit = 0, stand = 1;

    public static long money = 0;
    public static long moneyLo, moneyHi = 0;
    public static long shoeMoney = 0;
    public static long shoeMoneyLo, shoeMoneyHi = 0;
    public static int numHands = 0;
    public static int lastBet = 0;
    public static boolean debug = false;
    public static void updateMoney(int change) {
        money+=change;
        shoeMoney+=change;
        if(money<moneyLo) moneyLo = money;
        else if(money>moneyHi) moneyHi = money;
        if(shoeMoney<shoeMoneyLo) shoeMoneyLo = shoeMoney;
        else if(shoeMoney>shoeMoneyHi) shoeMoneyHi = shoeMoney;
    }
    public static void main(String[] args) throws IOException{
        // for(betSpreadNum = 0; betSpreadNum<8; betSpreadNum++) {
        //     betSpread = new int[3+betSpreadNum][2];
        //     betSpread[0][1] = 10;
        //     betSpread[0][0] = 1;
        //     betSpread[1][1] = 25;
        //     betSpread[1][0] = 1;
        //     for(int i = 2; i<betSpread.length; i++) {
        //         betSpread[i][0] = 1;
        //         betSpread[i][1] = Math.min(50*(i-1),300);
        //     }
        //     if(betSpreadNum>5) for(int i = 0; i<betSpread.length;i++) {
        //         betSpread[i][1]*=betSpreadNum-4;
        //     }
        //     if(betSpreadNum == 0) betSpread[2][1] = 75;
        //     for(int i = 0; i<betSpread.length;i++) {
        //         System.out.print("[");
        //         for(int j = 0; j<betSpread[i].length; j++) System.out.print(betSpread[i][j]+" ");
        //         System.out.print("] ");
        //     }
        //     System.out.println();
            simShoes(args);
        // }
    }
    public static void simShoes(String[] args) throws IOException{
        int numShoes = Integer.parseInt(args[0]);
        boolean recordShoes = false;
        if(args.length>1) countCards = Boolean.parseBoolean(args[1]);
        if(args.length>2) deviations = Boolean.parseBoolean(args[2]);
        if(args.length>3) recordShoes = Boolean.parseBoolean(args[3]);
        if(args.length>4) debug = Boolean.parseBoolean(args[4]);
        BufferedWriter writer = null;
        if(recordShoes)writer = new BufferedWriter(new FileWriter(new File("shoes"+betSpreadNum+".txt")));
        for(int i = 0; i<numShoes; i++) {
            if(numShoes>=100)if(i%(numShoes/100) == 0) System.out.println("betspread "+betSpreadNum+": "+(i/(numShoes/100))+"%");
            Shoe shoe = new Shoe();
            //lastBet = 0;
            while(shoe.shoe.size()>numDecks*52*deckPen && (!countCards || shoe.trueCount()>=minimumCount)) {
                simHand(shoe);
            }
            if(recordShoes) {
                writer.write(shoeMoney+","+shoeMoneyHi+","+shoeMoneyLo+"\n");
                shoeMoney = shoeMoneyHi = shoeMoneyLo = 0;
            }
        }
        if(recordShoes)writer.close();
        //System.out.println("true1 average: "+((double)true1money/numtrue1Counts));
        System.out.println("money made: "+(double)money+"\ndollars per shoe "+((double)money/numShoes)+"\ndollars per hand "+((double)money/numHands)+"\nhands per shoe "+((double)numHands/numShoes));
        money = 0;
        numShoes = 0;
        numHands = 0;
    }
    //static float expectedInsuranceValue = 0;
    //static long insuranceMoney = 0;
    //static long insuranceBets = 0;
    // static long numtrue1Counts, true1money, prevMoney;
    // static boolean wasTrue1;
    public static void simHand(Shoe shoe) {
        if(debug)System.out.println(money+"\n");
        numHands++;
        int bet, trueCount = shoe.trueCount();
        if(trueCount<=0 || !countCards) {
            bet = betSpread[0][1];
        } else if(trueCount>=betSpread.length) {
            bet = betSpread[betSpread.length-1][1];
        } else {
            bet = betSpread[trueCount][1];
        }
        bet = Math.max(Math.min(lastBet+gradualBetChange,bet), lastBet-gradualBetChange);
        lastBet = bet;
        Spot player = new Spot(shoe,bet);
        updateMoney(-bet);
        Hand dealer = new Hand(shoe);
        dealer.addCard(shoe.drawCard());
        dealer.addCard(shoe.drawCardFaceDown());
        player.hands.get(0).addCard(shoe.drawCard());
        player.hands.get(0).addCard(shoe.drawCard());
        //insurance
        if(countCards && deviations && dealer.cards.get(0) == blackjack.ace && shoe.trueCount()>=3 ) {
            updateMoney(-bet/2);
            //insuranceBets++;
            //expectedInsuranceValue+=shoe.insuranceEV();
            //insuranceMoney-=1;
            if(dealer.cards.get(1) == 10) {
                updateMoney(3*bet/2);
                //insuranceMoney+=3;
            }
        }
        //dealer blackjack
        if(dealer.isBlackJack()) {
            if(player.hands.get(0).isBlackJack()) {
                updateMoney(bet);
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
            updateMoney(bet*5/2);
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
                player.hands.add(new Hand(shoe,bet,player));
                updateMoney(-bet);
                player.hands.get(player.hands.size()-1).addCard(player.hands.get(handNum).cards.remove(1));
                player.hands.get(handNum).addCard(shoe.drawCard());
                player.hands.get(player.hands.size()-1).addCard(shoe.drawCard());
            } else handNum++;
        }
        //surrenders
        if(player.hands.size() == 1 && basicStrategy.surrender(player.hands.get(0).hardTotal(),dealer.cards.get(0),shoe)) {
            updateMoney(bet/2);
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
                    player.hands.get(i).bet*=2;
                    player.hands.get(i).addCard(shoe.drawCard());
                    player.hands.get(i).done = true;
                    updateMoney(-bet);
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
                updateMoney(2*player.hands.get(i).bet);
                continue;
            }
            int bestTotal = hardTotal;
            int dealerBestTotal = dealerHardTotal;
            if(player.hands.get(i).hasAce && hardTotal<=11) bestTotal = hardTotal+10;
            if(dealer.hasAce && dealerHardTotal<=11) dealerBestTotal = dealerHardTotal+10;
            if(dealerBestTotal<bestTotal){
                updateMoney(2*player.hands.get(i).bet);
                continue;
            }
            if(dealerBestTotal==bestTotal){
                updateMoney(player.hands.get(i).bet);
                continue;
            }
        }
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
    int bet;
    Shoe shoe;
    boolean hasAce, done;
    Spot spot;
    public Hand(Shoe shoe, int bet, Spot spot) {
        this.shoe = shoe;
        this.bet = bet;
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
    public Spot(Shoe shoe, int bet) {
        this.hands = new ArrayList<Hand>();
        this.insurance = false;
        this.surrender = false;
        this.shoe = shoe;
        hands.add(new Hand(shoe, bet, this));
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