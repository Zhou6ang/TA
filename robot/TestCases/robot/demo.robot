*** Variables ***
${para1}
${para2}
*** Test Cases ***
just fake test case
    keyword 1
    Log    ${para1}
    Log    ${para2}    
*** Keywords ***
keyword 1
    ${para2}=    Set Variable    1000
    Set Variable    ${para1}    10
    Set Test Variable    ${para2}    
