# lsa2odm
Convert Limesurvey Archives (.lsa files, a combination of surveys and responses) to CDISC ODM (Operational Data Model)

## Bachelor
This repo is the implementation for my bachelors thesis "Design of a mapping and implementation of a converter from LimeSurvey surveys and responses to CDISC ODM"

## Input and Output (During development)

All input files are located in src/main/xml.

The input file can be changed by editing the "filename" variable in src/main/java/app/App.java.

All input files have a "\_example" extension for lss and a "\_responses" extension for lsr, those are added automatically and not part of the filename variable.

So for an input file "ls-file_example.lss" and "ls-file_responses.lsr" filename would be "ls-file".

The output file is odm.xml in src/main/xml.
