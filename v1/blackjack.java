import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

class Shoe {
    public int min, max, start, end;
    public int count;
    public Queue<Integer> remainingCards;
    public Shoe() {
        this.min = 0;
        this.max = 0;
        this.start = 0;
        this.end = 0;
        count = 0;
        ArrayList<Integer> shoe = new ArrayList<>();
        for(int i = 0; i<blackjack.numDecks*4; i++) {
            for(int v = 2; v<12; v++) {
                shoe.add(v);
            }
            for(int j = 0; j<3; j++) {
                shoe.add(10);
            }
        }
        Collections.shuffle(shoe, new Random(39864));
        remainingCards = new LinkedList<Integer>(shoe);
    }
    public Shoe(int start) {
        this.min = 0;
        this.max = 0;
        this.start = start;
        this.end = 0;
        count = 0;
        ArrayList<Integer> shoe = new ArrayList<>();
        for(int i = 0; i<blackjack.numDecks*4; i++) {
            for(int v = 2; v<12; v++) {
                shoe.add(v);
            }
            for(int j = 0; j<3; j++) {
                shoe.add(10);
            }
        }
        Collections.shuffle(shoe, new Random(39864));
        remainingCards = new LinkedList<Integer>(shoe);
    }
    public Shoe(int min, int max, int start, int end) {
        this.min = min;
        this.max = max;
        this.start = start;
        this.end = end;
        count = 0;
        ArrayList<Integer> shoe = new ArrayList<>();
        for(int i = 0; i<blackjack.numDecks*4; i++) {
            for(int v = 2; v<12; v++) {
                shoe.add(v);
            }
            for(int j = 0; j<3; j++) {
                shoe.add(10);
            }
        }
        Collections.shuffle(shoe);
        remainingCards = new LinkedList<Integer>(shoe);
    }
    public int trueCount() {
        //true count = running count / decks rounded to the nearest half deck
        return 2*count/(int)(Math.round(remainingCards.size()/26.0));
    }
    public int drawCard() {
        int nextCard = remainingCards.poll();
        if(nextCard>9) count--;
        else if(nextCard<7) count++;
        return nextCard;
    }
    public void update(int change) {
        end+=change;
        if(min>end) min = end;
        if(max<end) max = end;
    }
    public String toString() {
        if(start == 0) return min+","+max+","+end;
        else return min+","+max+","+start+","+end;
    }
}
class hand {
    ArrayList<Integer> cards;
    ArrayList<hand> connectedHands;
    int numSplits;
    int bet;
    boolean surrender;
    Shoe shoe;
    boolean cardCounter;
    public hand(int numSplits, ArrayList<hand> connectedHands, Shoe shoe, int bet) {
        this.cards = new ArrayList<Integer>();
        this.connectedHands = connectedHands;
        this.numSplits = numSplits;
        this.bet = bet;
        this.surrender = false;
        this.shoe = shoe;
        cardCounter = blackjack.countCards;
    }
    public hand(int numSplits, ArrayList<hand> connectedHands, Shoe shoe) {
        this.cards = new ArrayList<Integer>();
        this.connectedHands = connectedHands;
        this.numSplits = numSplits;
        this.bet = blackjack.betSpread[0][0];//minimum bet
        this.surrender = false;
        this.shoe = shoe;
        cardCounter = false;
    }
    public boolean isBlackjack() {
        if(cards.size() != 2) return false;
        if(cards.get(0) == blackjack.ace) return cards.get(1) == 10;
        else return cards.get(0) == 10 && cards.get(1) == blackjack.ace;
    }
    public int getSoftTotal() {
        boolean hasAce = false;
        int softCount = 0;
        for(int i : cards) {
            if(i == blackjack.ace) {
                if(hasAce) softCount++;
                else hasAce = true;
            }
            else softCount+=i;
        }
        if(hasAce) return softCount;
        else return -1;
    }
    public int getHardTotal() {
        int ct = 0;
        for(int i : cards) {
            if(i == blackjack.ace)ct++;
            else ct+=i;
        }
        return ct;
    }
    public boolean canSplit() {
        if(cards.size() != 2 || cards.get(0) != cards.get(1)) return false;
        else if(numSplits >= blackjack.maxSplits) return false;
        else if(numSplits > 0  && !blackjack.multipleAceSplits && cards.get(0) == blackjack.ace) return false;
        else return true;
    }
    public boolean canDouble() {
        int playerVal = getHardTotal();
        return blackjack.minDouble <= playerVal && playerVal <= blackjack.maxDouble && (blackjack.das || numSplits == 0) && cards.size()<3;
    }
    public boolean isSplitAce() {
        return numSplits>0 && cards.get(0) == 11;
    }
    public int play(hand dealerHand) {
        int dealerVal = dealerHand.cards.get(0);
        int softTotal = getSoftTotal();
        if(canSplit()) if(split(dealerVal)) return blackjack.split;
        if(-1 < softTotal && softTotal < 10) {
            if(doubleSoft(dealerVal) && canDouble()) return blackjack.doubleYourBet;
            else if(standSoft(dealerVal) || isSplitAce()) return blackjack.stand;
            else return blackjack.hit;
        }
        else {
            if(doubleHard(dealerVal) && canDouble()) return blackjack.doubleYourBet;
            else if(standHard(dealerVal) || isSplitAce()) return blackjack.stand;
            else if(surrender(dealerHand)) return blackjack.surrender;
            else return blackjack.hit;
        }
    }
    public boolean split(int dealerVal) {
        int count = 0;
        if(cardCounter) count = shoe.trueCount();
        int playerVals = cards.get(0);
        if(playerVals == 5) return false;
        else if(playerVals == 8 || playerVals == 11) return true;
        else if(playerVals == 10) {
            if(4 <= dealerVal && dealerVal <= 6) return count>=10-dealerVal;
            else return false;
        }
        else if(playerVals == 6 && dealerVal == 2) return blackjack.das;
        else if(playerVals == 9 && dealerVal == 7) return false;
        else if(playerVals == 4) return (dealerVal == 5 || dealerVal == 6) && blackjack.das;
        else if(playerVals <= 3) {
            if(dealerVal <= 3) return blackjack.das;
            else return dealerVal<=7;
        }
        return dealerVal <= playerVals;
    }
    public boolean doubleSoft(int dealerVal) {
        int count = 0, runningCount = 0;
        if(cardCounter) {
            count = shoe.trueCount();
            runningCount = shoe.count;
        }
        int playerVal = getSoftTotal();
        if(dealerVal>6) return false;
        else if(playerVal>8) return false;
        else if(playerVal == 7) return true;
        else if(playerVal<=3) return dealerVal>=5;
        else if(playerVal<=5) return dealerVal>=4;
        else if(playerVal==6) {
            if(dealerVal>2) return true;
            else return count>=1;
        }
        else if(dealerVal == 6)  {
            if(blackjack.hitSoft17) return runningCount>=0;
            else return count>=1;
        }
        else if(dealerVal == 5) return count>=1;
        else if(dealerVal == 4) return count>=3;
        return false;
    }
    public boolean doubleHard(int dealerVal) {
        int count = 0;
        if(cardCounter) count = shoe.trueCount();
        int playerVal = getHardTotal();
        if(playerVal>11) return false;
        else if(playerVal<8) return false;
        else if(playerVal == 11) {
            if(blackjack.hitSoft17) return true;
            else return count>=1 || dealerVal<=10;
        }
        else if(playerVal == 10) {
            if(dealerVal<10) return true;
            else if(dealerVal == 10) return count>=4;
            else if(blackjack.hitSoft17) return count>=3;
            else return count>=4;
        }
        else if(playerVal == 9) {
            if(3<=dealerVal&&dealerVal<=6) return true;
            else if(dealerVal == 2) return count>=1;
            else if(dealerVal == 7) return count>=3;
            else return false;
        }
        else return dealerVal == 6 && count>=2;
    }
    public boolean standHard(int dealerVal) {
        int count = 0, runningCount = 0;
        if(cardCounter) {
            count = shoe.trueCount();
            runningCount = shoe.count;
        }
        int playerVal = getHardTotal();
        if(playerVal>16) return true;
        else if(playerVal<12) return false;
        else if(dealerVal>6) {
            if(dealerVal>8 && playerVal>14) {
                if(dealerVal == 9 && playerVal == 16) return count>=4;
                else if(dealerVal == 10 && playerVal == 16) return runningCount>0;
                else if(dealerVal == 10 && playerVal == 15) return count>=4;
                else if(blackjack.hitSoft17) {
                    if(dealerVal == 11) {
                        if(dealerVal == 16) return count>=3;
                        else return count>=5;
                    } else return false;
                }else return false;

            } else return false;
        } else {
            if(playerVal == 12) {
                if(dealerVal == 2) return count>=3;
                else if(dealerVal == 3) return count>=2;
                else if(dealerVal == 4) return runningCount>=0;
                else return true;
            }
            else return !(playerVal == 13 && dealerVal == 2 && count<=-1);
        }
    }
    public boolean standSoft(int dealerVal) {
        int playerVal = getSoftTotal();
        if(playerVal>7) return true;
        if(playerVal<7) return false;
        return (dealerVal<9);
    }
    public boolean surrender(hand dealerHand) {
        int dealerVal = dealerHand.cards.get(0);
        int playerVal = getHardTotal();
        if(!blackjack.lateSurrender || dealerVal<8 || playerVal>17 || playerVal<15 || cards.size()>2 || numSplits>0) return false;
        if(playerVal == 17 && (!blackjack.hitSoft17 || dealerVal < 11)) return false;
        int count = 0, runningCount = 0;
        if(cardCounter) {
            count = shoe.trueCount();
            runningCount = shoe.count;
        }
        if(playerVal == 15) {
            if(dealerVal == 10) return runningCount>=0;
            else if(dealerVal == 8)return false;
            else if(blackjack.hitSoft17) return count>=2;
            else if(dealerVal == 11) return count>=-1;
            else return count>=2;
        }
        else {
            if(dealerVal == 8) return count>=4;
            else if(dealerVal == 9) return count>-1;
            else return true;
        }
    }
    public boolean insurance(int dealerVal) {
        return dealerVal == 11 && shoe.trueCount()>=3;
    }
}

public class blackjack {
    public static int numDecks = 6;
    public static int numOtherPlayers = 0;
    public static double deckPen = 0.25;
    public static boolean hitSoft17 = true;
    public static boolean das = true;
    public static int minDouble = 0, maxDouble = 21;
    public static int maxSplits = 3;
    public static boolean multipleAceSplits = true;
    public static boolean lateSurrender = true;
    //betspread[true count][0] spots of $betspread[true count][1]
    public static int[][] betSpread = {{1,10}, {1,25}, {1,40}, {2,25}, {2,40}, {2,50}, {2,60}};
    public static int minimumCount = -1;
    public static boolean countCards = false;
    public static boolean debug = false;

    public static final int ace = 11;

    public static int numHands = 0;

    public static void main(String[] args) {
        if(args.length>0) countCards = Boolean.parseBoolean(args[0]);
        if(args.length>1) debug = Boolean.parseBoolean(args[1]);
        if(!countCards) minimumCount = -100;

        ArrayList<Integer> changes = new ArrayList<>();
        for(int i = 0; i<1; i++) {
            //if(i%10000 == 0) System.out.println(i);
            Shoe shoe = new Shoe();
            while(shoe.remainingCards.size()>numDecks*52*deckPen && shoe.trueCount()>=minimumCount) {
                simHand(shoe);
            }
            changes.add(shoe.end);
            lastAmount = 0;
        }
        int total = 0;
        for(int c : changes) total+=c;
        System.out.println((double)total/changes.size()+" "+((double)total/numHands));
    }
    public static int lastAmount = 0;

    public static int simHand(Shoe shoe) {
        if(debug) {
            System.out.println(shoe.end-lastAmount+"\n");
            lastAmount = shoe.end;
        }
        ArrayList<hand> hands = new ArrayList<>();
        int trueCount = shoe.trueCount();
        int numSpots, bet;
        if(trueCount<0 || !countCards) {
            numSpots = betSpread[0][0];
            bet = betSpread[0][1];
        }
        else if(trueCount>=betSpread.length) {
            numSpots = betSpread[betSpread.length-1][0];
            bet = betSpread[betSpread.length-1][1];
        }
        else {
            numSpots = betSpread[trueCount][0];
            bet = betSpread[trueCount][1];
        }
        for(int i = 0; i<numOtherPlayers+numSpots; i++) {
            hands.add(new hand(0,new ArrayList<hand>(),shoe,bet));
            hands.get(i).cards.add(shoe.drawCard());
            hands.get(i).cards.add(shoe.drawCard());
        }
        hand dealerHand = new hand(0,new ArrayList<hand>(),shoe);
        dealerHand.cards.add(shoe.drawCard());
        dealerHand.cards.add(shoe.drawCard());
        if(dealerHand.isBlackjack()){
            int moneySpent = 0, moneyMade = 0;
            for(int i = 0; i<numSpots; i++) {
                moneySpent+=hands.get(i).bet;
                if(hands.get(i).isBlackjack()) moneyMade+=hands.get(i).bet;
            }
            shoe.update(moneyMade);
            shoe.update(-moneySpent);
            numHands++;
            return moneyMade-moneySpent;
        }
        for(int i = 0; i<numOtherPlayers+numSpots; i++) {
            boolean done = false;
            while(!done && hands.get(i).getSoftTotal() != 10 && hands.get(i).getHardTotal()<21) {
                int play = hands.get(i).play(dealerHand);
                if(play == hit) {
                    hands.get(i).cards.add(shoe.drawCard());
                    System.out.println("hit");
                }
                else if(play == stand) {
                    done = true;
                    System.out.println("stand");
                }
                else if(play == doubleYourBet) {
                    if(i<numSpots) hands.get(i).bet*=2;
                    hands.get(i).cards.add(shoe.drawCard());
                    done = true;
                    System.out.println("double");
                }
                else if(play == split) {
                    if(i<numSpots) {
                        numSpots++;
                        hands.add(i+1, new hand(hands.get(i).numSplits+1, new ArrayList<hand>(hands.get(i).connectedHands),shoe,bet));
                    } else hands.add(i+1, new hand(hands.get(i).numSplits+1, new ArrayList<hand>(hands.get(i).connectedHands),shoe));
                    hands.get(i+1).connectedHands.add(hands.get(i));
                    for(hand h : hands.get(i+1).connectedHands) {
                        h.connectedHands.add(hands.get(i+1));
                        h.numSplits++;
                    }
                    hands.get(i+1).cards.add(hands.get(i).cards.remove(0));
                    hands.get(i).cards.add(shoe.drawCard());
                    hands.get(i+1).cards.add(shoe.drawCard());
                    System.out.println("split");
                } else if(play ==  surrender) {
                    done = true;
                    hands.get(i).surrender = true;
                    System.out.println("surrender");
                }
            }
        }
        boolean done = false;
        while(!done) {
            if(dealerHand.getHardTotal()>16) done = true;
            int dealerSoftTotal = dealerHand.getSoftTotal();
            if(dealerSoftTotal>10);
            else if(hitSoft17) {
                if(dealerSoftTotal>6) done = true;
            }
            else if(dealerSoftTotal>5) {
                done = true;
            }
            if(!done) dealerHand.cards.add(shoe.drawCard());
        }
        int dealerSoftTotal = dealerHand.getSoftTotal(), dealerHardTotal = dealerHand.getHardTotal(), dealerTotal = -1;
        if(-1 < dealerSoftTotal && dealerSoftTotal <=10) dealerTotal = dealerSoftTotal+ace;
        else dealerTotal = dealerHardTotal;
        int moneySpent = 0, moneyMade = 0;
        for(int i = 0; i<numSpots; i++) {
            moneySpent+=hands.get(i).bet;
            //System.out.println("spent "+hands.get(i).bet);
            int softTotal = hands.get(i).getSoftTotal();
            int hardTotal = hands.get(i).getHardTotal();
            if(debug) {
                System.out.print(dealerHand.cards.get(0)+":");
                for(int j = 0; j<hands.get(i).cards.size(); j++) System.out.print(" "+hands.get(i).cards.get(j));
                System.out.print(",");
                for(int j = 0; j<dealerHand.cards.size(); j++) System.out.print(" "+dealerHand.cards.get(j));
                System.out.println();
            }
            if(hands.get(i).surrender) {
                //System.out.println("made "+(hands.get(i).bet/2));
                moneyMade+=hands.get(i).bet/2;
                continue;
            }
            else if(softTotal > -1 && softTotal<=10) {
                if(softTotal+ace == dealerTotal) {
                    moneyMade+=hands.get(i).bet;
                    //System.out.println("made "+(hands.get(i).bet));
                }
                else if(hands.get(i).isBlackjack() && hands.get(i).numSplits == 0) {
                    moneyMade += hands.get(i).bet*2.5;
                    //System.out.println("made "+(hands.get(i).bet*2.5));
                }
                else if(dealerTotal>21) {
                    moneyMade += hands.get(i).bet*2;
                    //System.out.println("made "+(hands.get(i).bet*2));
                }
                else if(softTotal+ace>dealerTotal){
                    moneyMade += hands.get(i).bet*2;
                    //System.out.println("made "+(hands.get(i).bet*2));
                }
            } else {
                if(hardTotal>21) continue;
                else if(dealerTotal>21) {
                    moneyMade += hands.get(i).bet*2;
                    //System.out.println("made "+(hands.get(i).bet*2));
                }
                else if(hardTotal>dealerTotal) {
                    moneyMade += hands.get(i).bet*2;
                    //System.out.println("made "+(hands.get(i).bet*2));
                }
                else if(hardTotal==dealerTotal) {
                    moneyMade += hands.get(i).bet;
                    //System.out.println("made "+(hands.get(i).bet));
                }
            }
        }
        // if(moneyMade-moneySpent<-40){
        //     System.out.println(moneyMade-moneySpent);
        //     while(true);
        // }
        shoe.update(-moneySpent);
        shoe.update(moneyMade);
        numHands++;
        return moneyMade-moneySpent;
    }

    public static final int hit = 0, stand = 1, doubleYourBet = 2, split = 3, surrender = 4;

}