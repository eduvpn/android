After do |scenario|
  if scenario.failed?
    $driver.save_screenshot("./features/screenshots/#{scenario.feature.name} - #{scenario.name} _FAIL.png")
  end
end