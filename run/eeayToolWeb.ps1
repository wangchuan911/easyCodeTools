$x = Split-Path -Parent $MyInvocation.MyCommand.Definition
$x = Split-Path -Parent $x
$toolJar = $x+'\target\demo-1.0.0-SNAPSHOT-fat.jar'
$toolconfr =$x+'\src\main\resources\conf\config.json'
&'C:\Program Files\Java\jdk1.8.0_121\bin\java.exe' -jar  $toolJar  run my.hehe.demo.ApplicationVerticle -conf $toolconfr
