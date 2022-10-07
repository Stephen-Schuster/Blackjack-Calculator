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
        Collections.shuffle(shoe,new Random(45907093));
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
        Collections.shuffle(shoe);
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
        return blackjack.numDeckEstimationDivisions*count/(blackjack.numDeckEstimationDivisions*remainingCards.size()/52);
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
class spot {
    ArrayList<hand> hands;
    int moneySpent, moneyMade;
    boolean insurance, surrender;
    Shoe shoe;
    public spot(Shoe shoe, int bet) {
        this.hands = new ArrayList<hand>();
        this.moneySpent = 0; 
        this.moneyMade = 0;
        this.hands.add(new hand(shoe,bet,this));
        this.insurance = false;
        this.shoe = shoe;
    }
    public boolean insurance() {
        return blackjack.countCards && shoe.trueCount()>=3;
    }
}
class hand {
    ArrayList<Integer> cards;
    int bet;
    Shoe shoe;
    boolean hasAce;
    spot spot;
    public hand(Shoe shoe, int bet, spot spot) {
        this.cards = new ArrayList<Integer>();
        this.bet = bet;
        if(spot != null) spot.moneySpent+=bet;
        this.shoe = shoe;
        this.hasAce = false;
        this.spot = spot;
    }
    public void addCard(int card) {
        cards.add(card);
        hasAce = hasAce || card == blackjack.ace;
    }
    public boolean isBlackjack() {
        if(spot == null) return (cards.get(0) == blackjack.ace && cards.get(1) == 10) || (cards.get(1) == blackjack.ace && cards.get(0) == 10);
        else return spot.hands.size() == 1 && ((cards.get(0) == blackjack.ace && cards.get(1) == 10) || (cards.get(1) == blackjack.ace && cards.get(0) == 10));
    }
    // public int getSoftTotal(int handNum) {
    //     boolean hasAce = false;
    //     int softCount = 0;
    //     for(int i : cards.get(handNum)) {
    //         if(i == blackjack.ace) {
    //             if(hasAce) softCount++;
    //             else hasAce = true;
    //         }
    //         else softCount+=i;
    //     }
    //     if(hasAce) return softCount;
    //     else return -1;
    // }
    public int getHardTotal() {
        int ct = 0;
        for(int i : cards) {
            if(i == blackjack.ace)ct++;
            else ct+=i;
        }
        return ct;
    }
    public boolean canContinue() {
        return getHardTotal()<21 && !(hasAce && getHardTotal() == 11);
    }
    public boolean canSplit() {
        return spot.hands.size()<blackjack.maxSplitHands && 
        cards.get(0) == cards.get(1) && 
        cards.size() == 2 &&
        !(!blackjack.resplitAces && spot.hands.size()>1 && cards.get(0) == blackjack.ace);
    }
    public boolean canDouble() {
        int playerVal = getHardTotal();
        return blackjack.minDouble <= playerVal && playerVal <= blackjack.maxDouble && (blackjack.das || spot.hands.size() == 1) && cards.size()<=2;
    }
    public boolean doubleDown(int dealerVal) {
        if(hasAce) return doubleSoft(dealerVal);
        else return doubleHard(dealerVal);
    } 
    public boolean stand(int dealerVal) {
        if(hasAce) return standSoft(dealerVal);
        else return standHard(dealerVal);
    } 
    public int play(hand dealerHand) {
        int dealerVal = dealerHand.cards.get(0);
        if(canSplit()) {
            if(split(dealerVal)) return blackjack.split;
        }
        if(spot.hands.size()>1 && cards.get(0) == blackjack.ace) return blackjack.stand;
        if(canDouble()) {
            if(doubleDown(dealerVal)) return blackjack.doubleDown;
        }
        if(stand(dealerVal)) return blackjack.stand;
        else return blackjack.hit;
    }
    public boolean split(int dealerVal) {
        int count = 0;
        if(blackjack.countCards) count = shoe.trueCount();
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
        if(blackjack.countCards) {
            count = shoe.trueCount();
            runningCount = shoe.count;
        }
        int playerVal = getHardTotal()-1;
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
        if(blackjack.countCards) count = shoe.trueCount();
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
        if(blackjack.countCards) {
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
        int playerVal = getHardTotal()-1;
        if(playerVal>7) return true;
        if(playerVal<7) return false;
        return (dealerVal<9);
    }
    public boolean surrender(hand dealerHand) {
        int dealerVal = dealerHand.cards.get(0);
        int playerVal = getHardTotal();
        if(dealerVal<8 || playerVal>17 || playerVal<15 || (playerVal == 17 && (!blackjack.hitSoft17 || dealerVal < 11))) return false;
        int count = 0, runningCount = 0;
        if(blackjack.countCards) {
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
}

public class blackjack {
    public static int numDecks = 6;
    public static int numOtherPlayers = 0;
    public static double deckPen = 0.25;
    public static boolean hitSoft17 = true;
    public static boolean das = true;
    public static int minDouble = 0, maxDouble = 21;
    public static int maxSplitHands = 3;
    public static boolean resplitAces = true;
    public static boolean lateSurrender = true;
    public static int numDeckEstimationDivisions = 2;
    //betspread[true count][0] spots of $betspread[true count][1]
    public static int[][] betSpread = {{1,10}, {1,10}, {1,40}, {2,40}, {2,80}, {2,150}, {2,200}};
    public static int minimumCount = -1;
    public static boolean countCards = false;
    public static boolean debug = false;

    public static final int ace = 11;

    public static int numHands = 0;

    public static int numHighCountRounds, numHighCountWins, numLowCountRounds, numLowCountWins;

    public static void main(String[] args) {
        if(args.length>0) countCards = Boolean.parseBoolean(args[0]);
        if(args.length>1) debug = Boolean.parseBoolean(args[1]);
        if(!countCards) minimumCount = -100;

        ArrayList<Integer> changes = new ArrayList<>();
        for(int i = 0; i<1; i++) {
            if(i%10000 == 0) System.out.println(i);
            Shoe shoe = new Shoe();
            while(shoe.remainingCards.size()>numDecks*52*deckPen && shoe.trueCount()>=minimumCount) {
                simulateHand(shoe);
            }
            changes.add(shoe.end);
            lastAmount = 0;
        }
        int total = 0;
        for(int c : changes) total+=c;
        System.out.println((double)numHighCountWins/numHighCountRounds+" "+((double)numLowCountWins/numLowCountRounds));
        System.out.println((double)total/changes.size()+" "+((double)total/numHands));
    }
    public static int lastAmount = 0;

    public static int simulateHand(Shoe shoe) {
        ArrayList<spot> spots = new ArrayList<>();
        int trueCount = shoe.trueCount();
        boolean highCount = trueCount>1;        
        //if(debug) System.out.println(trueCount+" "+shoe.remainingCards.size()+" "+shoe.count);
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
        hand dealerHand = new hand(shoe,0,null);
        dealerHand.addCard(shoe.drawCard());
        dealerHand.addCard(shoe.drawCard());
        for(int i = 0; i<numOtherPlayers+numSpots; i++) {
            spots.add(new spot(shoe, bet));
            spots.get(i).hands.get(0).addCard(shoe.drawCard());
            spots.get(i).hands.get(0).addCard(shoe.drawCard());
        }
        for(int i = 0; i<numSpots; i++) {
            if(dealerHand.cards.get(0) == blackjack.ace && spots.get(i).insurance()) {
                spots.get(i).insurance = true;
                spots.get(i).moneySpent+=spots.get(i).hands.get(0).bet/2;
                if(debug)System.out.println("insurance");
            }
        }
        if(dealerHand.isBlackjack()) {
            for(int i = 0; i<numSpots; i++) {
                if(spots.get(i).hands.get(0).isBlackjack()) spots.get(i).moneyMade+=spots.get(i).hands.get(0).bet;
                if(spots.get(i).insurance) spots.get(i).moneyMade+=3*spots.get(i).hands.get(0).bet/2;
            }
        } else {
            for(int i = 0; i<numSpots+numOtherPlayers; i++) {
                if(spots.get(i).hands.get(0).surrender(dealerHand)){
                    if(debug)System.out.println("surrender");
                    spots.get(i).surrender = true;
                    spots.get(i).moneyMade+=spots.get(i).hands.get(0).bet/2;
                }
            }
            for(int i = 0; i<numSpots+numOtherPlayers; i++) {
                if(spots.get(i).surrender) continue;
                for(int j = 0; j<spots.get(i).hands.size(); j++) {
                    boolean done = false;
                    while(!done && spots.get(i).hands.get(j).canContinue()) {
                        int play = spots.get(i).hands.get(j).play(dealerHand);
                        if(play == hit) {
                            if(debug)System.out.println("hit");
                            spots.get(i).hands.get(j).addCard(shoe.drawCard());
                        }
                        else if(play == stand) {
                            if(debug)System.out.println("stand");
                            done = true;
                        }
                        else if(play == doubleDown) {
                            if(debug)System.out.println("double");
                            spots.get(i).moneySpent+=spots.get(i).hands.get(j).bet;
                            spots.get(i).hands.get(j).bet*=2;
                            spots.get(i).hands.get(j).addCard(shoe.drawCard());
                            done = true;
                        }
                        else if(play == split) {
                            if(debug)System.out.println("split");
                            spots.get(i).hands.add(new hand(shoe, spots.get(i).hands.get(j).bet, spots.get(i)));
                            spots.get(i).hands.get(spots.get(i).hands.size()-1).addCard(spots.get(i).hands.get(j).cards.remove(1));
                            spots.get(i).hands.get(spots.get(i).hands.size()-1).addCard(shoe.drawCard());
                            spots.get(i).hands.get(j).addCard(shoe.drawCard());
                        }
                    }
                }
            }
            boolean done = spots.get(0).surrender;
            while(!done) {
                int dealerHardTotal = dealerHand.getHardTotal();
                if(dealerHardTotal>16) done = true;
                else {
                    if(hitSoft17) {
                        if(dealerHand.hasAce && 8 <= dealerHardTotal && dealerHardTotal <= 11) done = true;
                    } else {
                        if(dealerHand.hasAce && 7 <= dealerHardTotal && dealerHardTotal <= 11) done = true;
                    }
                }
                if(!done) dealerHand.addCard(shoe.drawCard());
            }
            int dealerTotal = dealerHand.getHardTotal();
            if(dealerHand.hasAce && dealerTotal<=11) dealerTotal+=10;
            for(int i = 0; i<numSpots+numOtherPlayers; i++) {
                if(spots.get(i).surrender) continue;
                for(int j = 0; j<spots.get(i).hands.size(); j++) {
                    int handTotal = spots.get(i).hands.get(j).getHardTotal();
                    if(spots.get(i).hands.get(j).hasAce && handTotal<=11) handTotal+=10;
                    if(handTotal>21) continue;
                    else if(spots.get(i).hands.get(j).isBlackjack()) spots.get(i).moneyMade+=spots.get(i).hands.get(j).bet*2.5;
                    else if(dealerTotal>21 || handTotal>dealerTotal) spots.get(i).moneyMade+=spots.get(i).hands.get(j).bet*2;
                    else if(dealerTotal == handTotal) spots.get(i).moneyMade+=spots.get(i).hands.get(j).bet;
                }
            }
        }
        int totalMoneySpent = 0, totalMoneyMade = 0;
        for(int i = 0; i<numSpots; i++) {
            if(debug) {
                for(int h = 0; h<spots.get(i).hands.size(); h++) {
                    System.out.print(dealerHand.cards.get(0)+":");
                    for(int j = 0; j<spots.get(i).hands.get(h).cards.size(); j++) System.out.print(" "+spots.get(i).hands.get(h).cards.get(j));
                    System.out.print(",");
                    for(int j = 0; j<dealerHand.cards.size(); j++) System.out.print(" "+dealerHand.cards.get(j));
                    System.out.println();
                }
            }
            shoe.update(-spots.get(i).moneySpent);
            shoe.update(spots.get(i).moneyMade);
            totalMoneySpent+=spots.get(i).moneySpent;
            totalMoneyMade+=spots.get(i).moneyMade;
            numHands++;
        }
        if(debug)System.out.println(shoe.end+"\n\n\n");
        if(highCount) {
            numHighCountRounds++;
            numHighCountWins+=totalMoneyMade-totalMoneySpent;
        } else {
            numLowCountRounds++;
            numLowCountWins+=totalMoneyMade-totalMoneySpent;
        }
        return totalMoneyMade-totalMoneySpent;
    }

    // public static int simHand(Shoe shoe) {
    //     if(debug) {
    //         System.out.println(shoe.end-lastAmount+"\n");
    //         lastAmount = shoe.end;
    //     }
    //     ArrayList<hand> hands = new ArrayList<>();
    //     int trueCount = shoe.trueCount();
    //     int numSpots, bet;
    //     if(trueCount<0 || !countCards) {
    //         numSpots = betSpread[0][0];
    //         bet = betSpread[0][1];
    //     }
    //     else if(trueCount>=betSpread.length) {
    //         numSpots = betSpread[betSpread.length-1][0];
    //         bet = betSpread[betSpread.length-1][1];
    //     }
    //     else {
    //         numSpots = betSpread[trueCount][0];
    //         bet = betSpread[trueCount][1];
    //     }
    //     for(int i = 0; i<numOtherPlayers+numSpots; i++) {
    //         hands.add(new hand(0,new ArrayList<hand>(),shoe,bet));
    //         hands.get(i).cards.add(shoe.drawCard());
    //         hands.get(i).cards.add(shoe.drawCard());
    //     }
    //     hand dealerHand = new hand(0,new ArrayList<hand>(),shoe);
    //     dealerHand.cards.add(shoe.drawCard());
    //     dealerHand.cards.add(shoe.drawCard());
    //     if(dealerHand.isBlackjack()){
    //         int moneySpent = 0, moneyMade = 0;
    //         for(int i = 0; i<numSpots; i++) {
    //             moneySpent+=hands.get(i).bet;
    //             if(hands.get(i).isBlackjack()) moneyMade+=hands.get(i).bet;
    //         }
    //         shoe.update(moneyMade);
    //         shoe.update(-moneySpent);
    //         numHands++;
    //         return moneyMade-moneySpent;
    //     }
    //     for(int i = 0; i<numOtherPlayers+numSpots; i++) {
    //         boolean done = false;
    //         while(!done && hands.get(i).getSoftTotal() != 10 && hands.get(i).getHardTotal()<21) {
    //             int play = hands.get(i).play(dealerHand);
    //             if(play == hit) hands.get(i).cards.add(shoe.drawCard());
    //             else if(play == stand) done = true;
    //             else if(play == doubleYourBet) {
    //                 if(i<numSpots) hands.get(i).bet*=2;
    //                 hands.get(i).cards.add(shoe.drawCard());
    //                 done = true;
    //             }
    //             else if(play == split) {
    //                 if(i<numSpots) {
    //                     numSpots++;
    //                     hands.add(i+1, new hand(hands.get(i).numSplits+1, new ArrayList<hand>(hands.get(i).connectedHands),shoe,bet));
    //                 } else hands.add(i+1, new hand(hands.get(i).numSplits+1, new ArrayList<hand>(hands.get(i).connectedHands),shoe));
    //                 hands.get(i+1).connectedHands.add(hands.get(i));
    //                 for(hand h : hands.get(i+1).connectedHands) {
    //                     h.connectedHands.add(hands.get(i+1));
    //                     h.numSplits++;
    //                 }
    //                 hands.get(i+1).cards.add(hands.get(i).cards.remove(0));
    //                 hands.get(i).cards.add(shoe.drawCard());
    //                 hands.get(i+1).cards.add(shoe.drawCard());
    //             } else if(play ==  surrender) {
    //                 done = true;
    //                 hands.get(i).surrender = true;
    //             }
    //         }
    //     }
    //     boolean done = false;
    //     while(!done) {
    //         if(dealerHand.getHardTotal()>16) done = true;
    //         int dealerSoftTotal = dealerHand.getSoftTotal();
    //         if(dealerSoftTotal>10);
    //         else if(hitSoft17) {
    //             if(dealerSoftTotal>6) done = true;
    //         }
    //         else if(dealerSoftTotal>5) {
    //             done = true;
    //         }
    //         if(!done) dealerHand.cards.add(shoe.drawCard());
    //     }
    //     int dealerSoftTotal = dealerHand.getSoftTotal(), dealerHardTotal = dealerHand.getHardTotal(), dealerTotal = -1;
    //     if(-1 < dealerSoftTotal && dealerSoftTotal <=10) dealerTotal = dealerSoftTotal+ace;
    //     else dealerTotal = dealerHardTotal;
    //     int moneySpent = 0, moneyMade = 0;
    //     for(int i = 0; i<numSpots; i++) {
    //         moneySpent+=hands.get(i).bet;
    //         //System.out.println("spent "+hands.get(i).bet);
    //         int softTotal = hands.get(i).getSoftTotal();
    //         int hardTotal = hands.get(i).getHardTotal();
    //         if(debug) {
    //             System.out.print(dealerHand.cards.get(0)+":");
    //             for(int j = 0; j<hands.get(i).cards.size(); j++) System.out.print(" "+hands.get(i).cards.get(j));
    //             System.out.print(",");
    //             for(int j = 0; j<dealerHand.cards.size(); j++) System.out.print(" "+dealerHand.cards.get(j));
    //             System.out.println();
    //         }
    //         if(hands.get(i).surrender) {
    //             //System.out.println("made "+(hands.get(i).bet/2));
    //             moneyMade+=hands.get(i).bet/2;
    //             continue;
    //         }
    //         else if(softTotal > -1 && softTotal<=10) {
    //             if(softTotal+ace == dealerTotal) {
    //                 moneyMade+=hands.get(i).bet;
    //                 //System.out.println("made "+(hands.get(i).bet));
    //             }
    //             else if(hands.get(i).isBlackjack() && hands.get(i).numSplits == 0) {
    //                 moneyMade += hands.get(i).bet*2.5;
    //                 //System.out.println("made "+(hands.get(i).bet*2.5));
    //             }
    //             else if(dealerTotal>21) {
    //                 moneyMade += hands.get(i).bet*2;
    //                 //System.out.println("made "+(hands.get(i).bet*2));
    //             }
    //             else if(softTotal+ace>dealerTotal){
    //                 moneyMade += hands.get(i).bet*2;
    //                 //System.out.println("made "+(hands.get(i).bet*2));
    //             }
    //         } else {
    //             if(hardTotal>21) continue;
    //             else if(dealerTotal>21) {
    //                 moneyMade += hands.get(i).bet*2;
    //                 //System.out.println("made "+(hands.get(i).bet*2));
    //             }
    //             else if(hardTotal>dealerTotal) {
    //                 moneyMade += hands.get(i).bet*2;
    //                 //System.out.println("made "+(hands.get(i).bet*2));
    //             }
    //             else if(hardTotal==dealerTotal) {
    //                 moneyMade += hands.get(i).bet;
    //                 //System.out.println("made "+(hands.get(i).bet));
    //             }
    //         }
    //     }
    //     // if(moneyMade-moneySpent<-40){
    //     //     System.out.println(moneyMade-moneySpent);
    //     //     while(true);
    //     // }
    //     shoe.update(-moneySpent);
    //     shoe.update(moneyMade);
    //     numHands++;
    //     return moneyMade-moneySpent;
    // }

    public static final int hit = 0, stand = 1, doubleDown = 2, split = 3;

}