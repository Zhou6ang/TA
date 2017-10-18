*** Settings ***
Documentation    here is documentation
...              continue some description here.
Metadata    Version    2.0    #will show in suite description.
Metadata    More Info    For more information about *Robot Framework* see http://robotframework.org
Metadata    Executed At    ${HOST}
Default Tags    here is default tags    #make a tag and can be replaced by [Tags] inside of test case, which will be using in test case reporting.
Force Tags    here is force tags    #make a tag and cann't be replaced by [Tags] inside of test case, which will be using in test case reporting.
Library    ../libs/MyPythonKeywords.py    #import keywords(method) from outside of this file.
Variables    ../libs/MyVariables.py    #import variable from outside of this file.
Resource    ../libs/Resources.robot    #import resource from outside of this file.
Suite Setup    test suite setup    #only run once before test suite starting.
Suite Teardown    test suite teardown    #only run once after test suite ending.
Test Setup    test case setup of setting level    #effect for each test case and can be replaced by [Setup] inside of test case.
Test Teardown    test case teardown of setting level    #effect for each test case and can be replaced by [Teardown] inside of test case.
Test Timeout    20    #effect for each test case and can be replaced by [Timeout] inside of test case.
# Test Template    template keyword    #this will cause each keywords under the test case that will be looking at an input str parameter for this template keyword.


*** Variables ***
${para1}    #normal declare parameter.
${para2}
${expect}
&{newDict}    name=somebody    age=100    #dictionary type, which similar Map.
@{newList}    a    b    c    d    #arrays type, which similar with List.
${HOST}    127.0.0.1

*** Test Cases ***
example test case without template
    [Documentation]    some documentation and
    ...                description here.
    [Tags]    this is test case level tags
    [Setup]    test case level setup
    [Teardown]    test case level teardown
    [Timeout]    10
    Given keywords example    some input arg
    And key word "1"
    When key word "2"
    But key word "3"
    And key word "4"
    Then key word "5"
    And key word "6"
example test case with template
    [Documentation]    some documentation and
    ...                description here.
    [Tags]    this is test case level tags
    [Setup]    test case level setup
    [Teardown]    test case level teardown
    [Timeout]    10
    [Template]    template keyword
    Given key word "1"    
    When key word "2"
    Then key word "3"
first test case with normal bussiness 
    Given there are 2 parameters "10" and "20"
    When do adding them
    Then result is "30"
second test case with robot framework build-in function
    Given show built in function


*** Keywords ***
keywords example
    [Arguments]    ${arg}
    [Documentation]    here is keyword description.
    ...                continue description here.
    [Tags]    here is keyword tag
    [Timeout]    30
    [Teardown]    Log    here is keyword teardown
    Log    here is keyword action and input arg:${arg}.
    [Return]    "keyword return value"
key word "${index}"
    Log    here should have action in key word ${index}
    ${rlt}=    keywords example    ${index}
    Log    get result of keywords example ${rlt}
test case level setup
    Log    this is test case level setup
test case level teardown
    Log    this is test case level teardown
there are 2 parameters "${para1}" and "${para2}"
    Set Suite Variable    ${para1}    
    passing parameter to key word again "${para1}"
    Set Suite Variable    ${para2}    
    passing parameter to key word again "${para2}"
do adding them
    ${expect}=    Add    ${para1}    ${para2}
    Log    ${para1}    
    Log    ${para2}
    Log    ${expect}    
    Set Suite Variable    ${expect}    
passing parameter to key word again "${p}"
    Log    ${p}
result is "${result}"
    Should Be Equal As Integers    ${result}    ${expect}    
test suite setup
    ${time}=    Get Time    year month day hour min sec;
    Log    testsuite startup time:${time}
test suite teardown
    ${time}=    Get Time    year month day hour min sec;
    Log    testsuite teardown time:${time}
test case setup of setting level
    ${time}=    Get Time    year month day hour min sec;
    Log    testcase startup time:${time}
test case teardown of setting level
    ${time}=    Get Time    year month day hour min sec;
    Log    testcase teardown time:${time}
show built in function
    Log    here is value of dict:&{newDict}[name]
    Log    here is normal parameter:${expect}    
    Log    here is dictianary:&{newDict}        
    Log    here is list:@{newList}
    Log    here will show for loop:
    @{myList} =    Create List    a    b    c    d    e
    :FOR    ${i}    IN    @{myList}
    # \    simple IFESLE statement is constructed with Run Keyword If and  Run Keyword Unless
    \    Run Keyword If    '${i}'=='a'    Log    this is run_keywords_if:${i}
    \    Run Keyword Unless    '${i}'=='a'    Log    this is run_keyword_unless:${i}
    \    
    \    @{tmpTime}=    Run Keyword If    '${i}' == 'b'    Get Time    year month day hour min sec
    \    Log    current date here:@{tmpTime}
    \        
    
    Log    here is another ifelse statement:
    :FOR    ${j}    IN    @{myList}
    \    Run Keyword If    '${j}' == 'a'    Run Keywords      Log    doing some keywords action here with dedicate value:${j}    AND    Log    hahah
    \    ...    ELSE IF    '${j}' == 'b'    Log    doing other keywords action here with dedicate value:${j}.
    \    ...    ELSE IF    '${j}' == 'c'    Log    doing continue keywords here with dedicate value:${j}.
    \    ...    ELSE        Log    doing aother keywords action here with other value:${j}.
template keyword
    [Arguments]    ${p}
    Log    ${p}