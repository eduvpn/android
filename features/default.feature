Feature: default feature

  Scenario: Actionbar
	# As a user I can see the actionbar has the app title
    Given the app has started
    Then the actionbar shows "eduVPN"

  Scenario: Start
	# As a user I can see the header of the provider selection screen
    Given the app has started
    Then I see "Choose your provider"