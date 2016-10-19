require 'rubygems'
require 'selenium-cucumber'

capabilities = {
	platformName: 'Android',
	platformVersion: '5.1',
	deviceName: 'Android Emulator',
	app: './app/build/outputs/apk/app-debug.apk',
	appPackage: 'net.tuxed.vpnconfigimporter',
	appActivity: 'net.tuxed.vpnconfigimporter.MainActivity'
}

server_url = "http://127.0.0.1:4723/wd/hub"

begin
	$driver = Appium::Driver.new(caps: capabilities).start_driver
rescue Exception => e
	puts e.message
    Process.exit(0)  
end

# At the end disconnect the driver so the server can clean up
at_exit do
  $driver.quit
end