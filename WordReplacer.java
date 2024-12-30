import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 *  Class for a Word Replacer.
 * @author Kwabena G. Darko
 * @version 1.1.0 December 16, 2024
 */
public class WordReplacer {

    /**
     * Finds the replacement word of a given word.
     * This method repeatedly follows the parent references until it reaches
     * the root (final replacement word) which is identified as a node that does
     * not have a parent in the map.
     * @param node  starting word whose replacement is to be found
     * @param nodes a map representing words and their replacements
     * @return the replacement word of the input word
     */
    static String find(String node, MyMap<String, String> nodes) {
        //traverse down a word till we find the root(last representation in transitive dependencies)
        while(nodes.get(node) != null){
            node = nodes.get(node);
        }
        return node;
    }

    /**
     * Unites find data structure by merging two nodes (words).
     * This method ensures that the first node maps the root of the second
     * If merging the sets would create a cycle, an error is reported, and program exits.
     * An error is thrown and exits in failure if the operation creates a cycle,
     * indicated by attempting to unite a word with the same word as replacement
     * @param a     the first word to be mapped from.
     * @param b     the second word to be mapped to.
     * @param nodes a map representing words and their replacements
     */
    static void union(String a, String b, MyMap<String, String> nodes) {
        //find the last word for transitive dependencies
        String rootB = find(b, nodes);

        //if the final word after transitive dependencies is same as the first word there is a cycle
        if (a.equals(rootB)){
            System.err.println("Error: Cycle detected when trying to add replacement rule: " + a + " -> " + b);
            System.exit(1);
        }
        nodes.put(a, rootB);
    }

    public static void main(String[] args) {
        //exit in failure if commandline arguments are not enough
        if (args.length != 3) {
            System.err.println("Usage: java WordReplacer <input text file> <word replacements file> <bst|rbt|hash>");
            System.exit(1);
        }

        //exit in failure if the input file does not exist
        String inputTextFile = args[0];
        FileReader inputFile = null;
        try {
            inputFile = new FileReader(inputTextFile);
        }catch(FileNotFoundException e){
            System.err.println("Error: Cannot open file '" + inputTextFile + "' for input.");
            System.exit(1);
        }


        //exit in failure if the word replacements file does not exist
        String wordReplacementsFile = args[1];
        FileReader replacementFile = null;
        try {
            replacementFile = new FileReader(wordReplacementsFile);
        }catch(FileNotFoundException e){
            System.err.println("Error: Cannot open file '" + wordReplacementsFile + "' for input.");
            System.exit(1);
        }

        //exit in failure if an invalid data structure is used
        String dataStructure = args[2];
        if(!dataStructure.equals("bst") && !dataStructure.equals("rbt") && !dataStructure.equals("hash")){
            System.err.println("Error: Invalid data structure '" + dataStructure + "' received.");
            System.exit(1);
        }


        //create two map objects first one is for mapping each object
        //second one is for converting transitive dependencies and detecting cycles
        MyMap<String, String> map;
        MyMap<String, String> map1;
        if (dataStructure.equals("bst")){
            map = new BSTreeMap<>();
            map1 = new BSTreeMap<>();
        } else if (dataStructure.equals("rbt")) {
            map = new RBTreeMap<>();
            map1 = new RBTreeMap<>();
        } else{
            map = new MyHashMap<>();
            map1 = new RBTreeMap<>();
        }


        String a;
        String b;
        try{
            BufferedReader lineReader = new BufferedReader(replacementFile);
            String line = lineReader.readLine();
            while (line != null) {
                StringBuilder currentWord = new StringBuilder();
                StringBuilder replacemet = new StringBuilder();
                boolean isKey = true;

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    //check if the next characters are " ->"
                    if (c == ' ' && i + 2 < line.length()
                            && line.charAt(i + 1) == '-'
                            && line.charAt(i+2) == '>'){
                        isKey = false;
                        i+=2; // Skip the '->'
                        continue;
                    }
                    if (isKey) {
                        currentWord.append(c);
                    } else {
                        replacemet.append(c);
                    }
                }

                //convert keys(first word) and value(replacements) to String and put them in Map
                a = currentWord.toString().trim();
                b = replacemet.toString().trim();
                map.put(a,b);
                line = lineReader.readLine();
            }
        }catch (IOException e){
            System.err.println("Error: An I/O error occurred reading '" + wordReplacementsFile + "'.");
            System.exit(1);
        }

        /*
        this is a same try catch block as the previous one, this one checks for cycles,
        and maps words with transitive dependencies to their final word using union find
        this would prevent long lookup time for transitive dependencies when replacing words
        */
        try {
            BufferedReader lineReader2 = new BufferedReader(new FileReader(wordReplacementsFile));
            String line2 = lineReader2.readLine();
            while (line2 != null) {
                StringBuilder key = new StringBuilder();
                boolean isKey = true;

                for (int i = 0; i < line2.length(); i++) {
                    char c = line2.charAt(i);
                    if (c == ' ' && i + 2 < line2.length()
                            && line2.charAt(i + 1) == '-'
                            && line2.charAt(i + 2) == '>') {
                        isKey = false; // Switch to reading value
                        i+=2; // Skip the '>'
                        continue;
                    }
                    if (isKey) {
                        key.append(c);
                    }else {
                        break;
                    }
                }

                //this is where each key is mapped to the final word(if there is transitive dependencies
                //cycles are detected here
                a = key.toString().trim();
                union(a, find(a, map), map1);
                line2 = lineReader2.readLine();
            }
        }catch (IOException e){
            System.err.println("Error: An I/O error occurred reading '" + wordReplacementsFile + "'.");
            System.exit(1);
        }
        //replace the first map with the new map where transitive depencies have been resolved
        map = map1;

        //create a new string which holds the new output and a try catch block for the file to
        // be replaced with words
        StringBuilder modifiedOutput = new StringBuilder();
        try {
            BufferedReader replaceReader = new BufferedReader(inputFile);
            String replaceLine = replaceReader.readLine();
            while(replaceLine != null) {
                //this is the word we are building(any contiguous String with just letters)
                StringBuilder currentWord = new StringBuilder();
                for (int i = 0; i < replaceLine.length(); i++) {
                    char c = replaceLine.charAt(i);
                    //this checks if we have a word at the moment before we met a non letter character
                    if (!Character.isLetter(c)) {
                        //checks if non letter character has been met
                        if(!currentWord.isEmpty()) {
                            //if the word being built wasn't empty a string of that word is created
                            //then its replacement is searched for if there is one
                            //if there is none we append the word itself to the modified line
                            String currWord = currentWord.toString();
                            if (map.get(currWord) != null) {
                                modifiedOutput.append(map.get(currWord));
                            } else {
                                modifiedOutput.append(currentWord);
                            }
                            currentWord.setLength(0); //empty the StringBuilder Object
                        }
                        modifiedOutput.append(c);// adds the hanging non letter character to modified word
                    } else {
                        currentWord.append(c);
                    }
                }

                //this is used for handling last words of a line
                if(!currentWord.isEmpty()){
                    String currWord = currentWord.toString();
                    if (map.get(currWord) != null) {
                        modifiedOutput.append(map.get(currWord));
                    } else {
                        modifiedOutput.append(currWord);
                    }
                }
                modifiedOutput.append("\n");
                replaceLine = replaceReader.readLine();
            }
        }catch (IOException e) {
            System.err.println("Error: An I/O error occurred reading '" + inputTextFile + "'.");
            System.exit(1);
        }
        //print based on requirements given
        System.out.printf("%s\n", modifiedOutput);
    }
}