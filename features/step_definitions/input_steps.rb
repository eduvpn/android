require 'selenium-cucumber'

When /^I enter "([^\"]*)" into field with hint "([^\"]*)"$/ do |text, hint|
	enter_text(:xpath, text, "//android.widget.EditText[@text='#{hint}']")
end