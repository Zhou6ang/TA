# Robot
1. install python, download from https://www.python.org/
2. install jython, download from http://www.jython.org/
   command: java -jar jython-installer-2.7.0.jar
3. if needed, set https_proxy|http_proxy=https://0.0.0.0:8080. Then install robotframework
   if it's jython:
       command: /root/jython2.7.0/bin/easy_install robotframework
            or: /root/jython2.7.0/bin/pip install robotframework
   if it's only python:
       command: /root/Python35/Scripts/easy_install robotframework
            or: /root/Python35/Scripts/pip install robotframework
4. verify robotframework
   command with jython: /root/jython2.7.0/bin/robot --version
   command with python: /root/Python35/Scripts/robot --version
5. write a simple robot case with build-in library.
   check from /example folder
6. write a simple java library
