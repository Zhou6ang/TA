*** Settings ***
Library    String
Library    DateTime
Library    calculator.py
Library    MyLibrary

*** Variable ***
${para1}  ""
${para2}  ""

*** Test Cases ***
This is first robot test case
	input first string   abc
	input second string    EFG   
	expect result
	
The purpose of this case is testing DateTime Library
	${currentDate} =    Get Current Date
	Log To Console    current data is: ${currentDate}
	${date1} =     Convert Date    2014-06-11 10:07:42.000
	${date2} =     Convert Date    20140611 100742    result_format=timestamp
	Log To Console   my case output ${date1} vs ${date2}
	Should be equal   ${date1}   ${date2}
	
The purpose of this case is testing OperatingSystem Library
	${currentDate} =    Get Current Date
	Log To Console    current data is: ${currentDate}
	${date1} =     Convert Date    2014-06-11 10:07:42.000
	${date2} =     Convert Date    20140611 100742    result_format=timestamp
	Log To Console   my case output ${date1} vs ${date2}
	Should be equal   ${date1}   ${date2}
The purpose of this case is gherkin mode.
	Given prepare something here
	When input first number "1"
	And input second number "100"
	Then result is "101"
	And show us result in console

The purpose of this case is based on Java mode.
	Given input 2 parameter "10" "20"
	When execute keyword base on java
	Then expect is "30"
		
*** Keywords ***
input 2 parameter "${p1}" "${p2}"
	${rlt}=    calculator    ${p1}    ${p2}
	set test variable ${rlt}
execute keyword base on java
	log to console    here will do some data handling
expect is "${expect}"
	should be equal as integers    $expect    ${rlt}
	
	
prepare something here
	${total}=    Convert To Integer    0
	set test variable    ${total}
input first number "${para}"
	${p1} =    Convert To Integer    ${para}
	set test variable    ${p1}
	
input second number "${para}"
	${p2} =     Convert To Integer    ${para}
	set test variable    ${p2}
result is "${expect}"
	#${total} =    Evaluate    ${p1} + ${p2}
	${total} =    result    ${p1}    ${p2}
	log to console    ${total}
	set test variable    ${total}
	should be equal as integers    ${expect}    ${total}
show us result in console
	log to console    the operation result:${total}
	
	
input first string
	[Arguments]    ${str1}	
	${a}=    Convert To Uppercase   ${str1}
	Set Test Variable    ${a}
	#Should be equal    ${str}    ABCEFG
	
input second string
	[Arguments]    ${str1}
	${b}=    Convert To Lowercase    ${str1}
	Set Test Variable    ${b}
	
expect result	
	Should be equal    ${a}${b}    ABCefg   