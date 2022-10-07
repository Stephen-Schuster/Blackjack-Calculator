# Blackjack Calculator
## This repository has several versions of calculators for Blackjack. The 7th and final one is what's important
### This command line tool takes in 0 or more arguments, but here is the output with the default ones:

![output](demo.png)

### It simulates millions of hands of a card counter playing blackjack. The card counter employs [kelly betting](https://en.wikipedia.org/wiki/Kelly_criterion), [basic strategy](https://en.wikipedia.org/wiki/Blackjack#Basic_strategy), [card counting](https://en.wikipedia.org/wiki/Card_counting) and playing deviations to gain an edge on the house. The purpose of the simulaton is to calculate the approximate risk of ruin(or the chance that the card counter goes bankrupt) and expected value(or EV) of each hand in dollars at each level of bankroll. With default arguments, the card counter has a <1% risk of a ruin and it takes ~200k hands to 10x their bankroll.

# How to use
* Clone the repository and navigate to `Blackjack-Calculator/v7` in the terminal
* There are 3 valid commands: `simCareers`, `simShoes`, and `getBets`
* Run `java blackjack [command] [0 or more arguments]` and replace `[command]` with one of the 3 commands above and replace `[0 or more arguments]` with 0 or more arguments
* Arguments are formatted as `[argument name]=[argument value]`. There are 11 valid arguments, explained below

# Arguments
* `numShoes`: Number of shoes simulated when running the command `simShoes` or the maximum number of shoes in a career when running the command `simCareers`
* `bankroll`: The card counter's current bankroll(or starting bankroll for `simCareers`) for any command
* `successThresholdPerBankroll`: How much the card counter must multiply their starting bankroll by to be considered a successful career
* `numCareers`: The number of careers to simulate for the command `simCareers`
* `minimumEV`: The minimum expected return per hand a card counter is willing to earn. For example, if your expected return is $5 per hour, you might as well bet higher so that it's atleast worth your time, even if it's riskier. This only occurs if your bankroll gets too low
* `minimumBetPerBankroll`: What % of your bankroll you should bet when the deck is not in your favor. The lower this value is, the higher your expected return. The higher this value is, the less suspicion you'll attract from casinos.
* `numUpdates`: How many times do you want to be updated on the progress of the simulation. The program will display the its % completion this many times throughout the simulation
* `kellyBetMultiplier`: Determines how risky you want your bets to be. The higher this value, the higher your expected return but also the higher your risk of ruin
* `numProfitDivisions`: When you do the `simCareers` command, it displays your expected return at different ranges of bankroll. If you have more(and thus smaller) ranges, each range will be less accurate
* `tableMinimum`: The minimum amount that the casino allows you to bet
* `tableMaximum`: The maximum amount that the casino allows you to bet

# Default Arguments
* `numShoes`: infinite for `simCareers`, undefined for `simShoes`
* `bankroll`: $20,000
* `successThresholdPerBankroll`: 10
* `numCareers`: 1000
* `minimumEV`: 20 cents
* `minimumBetPerBankroll`: Depends on other arguments. If everything else is default, it is 0.000677
* `numUpdates`: 100
* `kellyBetMultiplier`: Depends on other arguments. If everything else is default, it is 0.356
* `numProfitDivisions`: 5
* `tableMinimum`: $10
* `tableMaximum`: $1000
