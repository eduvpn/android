require 'selenium-cucumber'

When /^I touch the "([^\"]*)" text$/ do |text|
	WAIT.until { $driver.find_element(:xpath, "//*[@text='#{text}']")}.click
end

When /^I click on "([^\"]*)"$/ do |text|
	WAIT.until { $driver.find_element(:xpath, "//*[@text='#{text}']")}.click
end

When /^I swipe "([^\"]*)" to the ([^\"]*)$/ do |text, direction|
	is_element_displayed(:xpath, "//*[@text='#{text}']")
	step %[I swipe element having xpath "//*[@text='#{text}']" to #{direction}]
	sleep 2
end