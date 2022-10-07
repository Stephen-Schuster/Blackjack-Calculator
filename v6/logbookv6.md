# Logbook for blackjack v6

Kelly betting is betting advantage/(standard deviation)^2 of your bankroll
ex: advantage of 1% and standard deviation of 1.15 means you should bet 0.76% of your bankroll

Step 1: calculate the probability distribution of each outcome for every positive count up to some maximum
Step 2: simulate a bunch of shoes and record the delta/bet for non-positive counts and the delta/bankroll on positive counts (We need to know the edge/standard distribution for this first!!!)
Step 3: Simulate a bunch of careers that start at a given bankroll and end when they either run out of money or get to a given higher bankroll

Things to remember:


Organization:
basic strategy class houses all basic strategy and deviation logic, same as v3
Spot holds mostly the same stuff as in v3, but also holds hand simulation logic, a method that returns the return on the bet(double between -8 and 8 inclusive)
Hand is the same as v3
Shoe holds the same stuff as v3 but also shoe simulation logic, loads and records stuff for steps 1 and 2
blackjack holds the rules and the main method for the command line


In going to use a random access file for getting random shoes because I think there will be too much data for putting it in an array. All the lines of profit and rent have to be a standard length(20)

I need to implement ror calculation and do the following: calculate minimum bankroll & minimum bets(step proportional to bankroll)