# AcmeLabs
Example phpBitsTheater framework website and mobile app which logs into it.

## Android App Development Setup (Android Studio) ##

1. Android Studio uses Gradle to build apps. A way to use the androidBits library with
your app is to edit the global `gradle.properties` file on your local computer.
Create this file in the Gradle user home directory, typically found on Windows 7+ at
`C:\Users\[user name]\.gradle` and on Linux or Mac OS X at `~/.gradle`.

2. Edit the new `gradle.properties` file to contain the following variables.
Note the lack of quotes and the full paths below, so paths with spaces may have unknown behavior:

  For Linux / OS X:
  ```
  lib_androidBits=/path/to/androidBits/lib_androidBits
  ```
  For Windows OS (yes, the extra backslashes are required):
  ```
  lib_androidBits=C\:\\path\\to\\androidBits\\lib_androidBits
  ```
  

## Website Development Setup (Eclipse) ##

1. Clone the phpBitsTheater repository in your development environment.

    ```
    git clone git+ssh://git@github.com/baracudda/phpBitsTheater ./phpBitsTheater
    ```

2. In Eclipse, create a new project for phpBitsTheater. 
3. Clone this repository in your development environment.

    ```
    git clone git+ssh://git@github.com/baracudda/acmelabs ./acmelabs
    ```

4. In Eclipse, create a new project for the `acmelabs/server`.
5. Right-click the project root in Eclipse, and select **Include Path** →
   **Configure Include Path…**
6. On the **Projects** tab, select the project for phpBitsTheater that was
   created in step 2.
7. Commit the changes to link the two projects.


## Website Installation ##

For a more detailed set of instructions on how to setup a website in a 
LAMP stack environment, see [this wiki page](https://github.com/baracudda/phpBitsTheater/wiki/Installation-into-a-LAMP-Stack).

1. Set up the database where the web service's data will be hosted.
2. Push the bare BitsTheater library to your target server instance.
3. Push your server code to the same location on the target server,
   overwriting any of the base files.
4. In a web browser, navigate to the root of the instance.
5. Follow instructions in the browser to install the service.
