require 'selenium-cucumber'

Given /^the app has started$/ do
	$driver.reset
end

Then /^the actionbar shows "([^\"]*)"$/ do |text|
    is_element_displayed(:uiautomator, "new UiSelector().text(\"#{text}\")")
end

Then /^I click on the home button/ do
    step %[I click on element having xpath "//*[@content-desc='Navigate up']"]
end

When /^I hide the keyboard$/ do
    step %[I navigate back]
end

