# lsa2odm
Convert Limesurvey Archives (.lsa files, a combination of surveys and responses) to CDISC ODM (Operational Data Model)

## Bachelor
This repo is the implementation for my bachelors thesis "Design of a mapping and implementation of a converter from LimeSurvey surveys and responses to CDISC ODM"

## Usage

### As a jar

You can use the jar to convert an archive. The first parameter must be the path to the archive, the second parameter is an optional output path.
Compiling the jar: 'mvn compile assembly:single', executed in the root directory of the project.
Usage: 'java -jar \<jar-name\> arg1 arg2'

### In another maven project

You can add this project to your own project as a dependency, then import the *LsaConverter* and use its *convert()* method.

### Example

You want to see, how this program works?

Executing *example.sh* runs the converter with an example survey, which is located under */src/main/xml/*.

## Converting the other way

If you want to convert ODM files to LSA, take a look at [odm2lsa](https://github.com/Delzye/odm2lsa).
**ATTENTION** The other converter is in a fairly basic state and cannot convert a lot of what ODM has to offer, only basic questions are converted for now.

## Missing features

- Encrypted answers to questions with a predefined list of answer options are considered invalid
- Multiple languages within a survey are not properly converted right now
- Some question types (text display, file upload, equation, ranking, language switch, browser detection) are not converted
- Different Date/Time-Formats from the default are not supported
- RegEx for input validation are not used as RangeChecks for now
- Predefined input lengths are not converted
