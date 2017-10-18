*** Settings ***
Library    zg.toolkit.pm.App

*** Variables ***
${username}    
${pwd}    
${host}    
${omesDir}    
${pmb_dir}
${supplFile}
${adaptationId}
${dn}

*** Test Cases ***
make pm data for adaptation
    Given prepare meta-data
    And check adaptation whether integrated
    When simulate pm data
    Then get correct records in DB
    
*** Keywords ***
prepare meta-data
    Set Test Variable    ${username}    omc
    Set Test Variable    ${pwd}    omc
    Set Test Variable    ${pmb_dir}    /media/sf_VirtualBoxSharedFolder/pm/adaptation_com.nsn.cscf.pmb-17.0VI-20170211T152226
    Set Test Variable    ${supplFile}    /media/sf_VirtualBoxSharedFolder/pm/adaptation_com.nsn.cscf.pmb-17.0VI-20170211T152226/com.nsn.cscf.supplementary.xml
    Set Test Variable    ${adaptationId}    com.nsn.cscf
    Set Test Variable    ${release}    17.0VI
    Set Test Variable    ${host}    10.92.18.156
    ${dn}=    get dn by "${adaptationId}"
    Set Test Variable    ${dn}
    Set Test Variable    ${omesDir}    /media/sf_VirtualBoxSharedFolder/pm
    
get dn by "${adaptationId}"
    
    [Return]    PLMN-AUTO/CSCF-ines-17.0VI
check adaptation whether integrated
    Log    ${adaptationId} already been integrated.
simulate pm data
    execute    ${pmb_dir}    ${supplFile}    ${dn}    ${username}    ${pwd}    ${host}    ${omesDir}
get correct records in DB
    Log    check record in DB.
