# configCollector
Collect configuration files on a Linux system into a zip file. And place them into the correct directory again later (on a new machine). 
No guaranty given, please read through the code before using it. 
## Usage
Used configuration: `config-data.json`. Form of that JSON file:
```json
{
  "target-path": "/your/awesome/path/to/place/zipFile/configs.zip",
  "config-files": ["/your/awesome/path/to/configFile/test_file_1.txt", "/another/awesome/path/to/configFile/test_file_2"]
}
```
where `target-path` is the file name including absolute path at which the zip file will be stored. `config-files` is
a list of file names (including the absolute path to the file). Each file specified by the its name in the list
will be collected and placed in the resulting zip-file.
Place the configuration file in the same directory as your created jar file and name it `config-data.json`. 
Run the jar file with `java -jar configCollector-1.0-standalone.jar` to collect your specified configuration files
and create a zip file at the specified target path. 
To place/extract the configuration files, to each directory from which it was collected with this tool,
run `java -jar configCollector-1.0-standalone.jar -c false -z your-config-zip-file.zip`.


## License

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
