require 'selenium-cucumber'

Then /^I see "([^\"]*)"$/ do |text|
	if not is_element_displayed(:uiautomator, "new UiSelector().text(\"#{text}\")")
		raise TestCaseFailed, "Element is not present"
	end
end

Then /^I dont see "([^\"]*)" anymore$/ do |text|
    fail_if_element_exists(:uiautomator, "new UiSelector().text(\"#{text}\")")
end

def fail_if_element_exists(access_type, access_name)
    if $driver.find_elements(:"#{access_type}","#{access_name}").size != 0
        raise TestCaseFailed, 'Element is present'
    end
end
