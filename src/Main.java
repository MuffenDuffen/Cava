import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    static class function{
        public String Name;
        public String ReturnType;
        public String[] Params;

        public function(String name, String returnType, String[] params)
        {
            Name = name;
            ReturnType = returnType;
            Params = params;
        }
    }

    private static function[] getNativeFunctions(String filename){
        File file = new File(filename);
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<function> functions = new ArrayList<>();
        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            if (line.contains(" native ")){
                int paramIndex = line.indexOf("(");
                int nameIndex = line.substring(0, paramIndex).lastIndexOf(" ");
                int typeIndex = line.substring(0, nameIndex).indexOf(" ");
                int closeIndex = line.indexOf(")");

                String parameter = line.substring(paramIndex + 1, closeIndex);

                String[] parameters = parameter.split(",");

                if(!parameter.isEmpty()) {
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = parameters[i].substring(0, parameters[i].indexOf(" "));
                    }
                }

                functions.add(new function(line.substring(nameIndex, paramIndex), line.substring(typeIndex, nameIndex), parameters));
            }
        }
        return functions.toArray(new function[]{});
    }

    // Getting an array of indexes that contains the find string
    private static Integer[] getIndexes(String[] lines, String find) {
        return (Integer[]) IntStream.range(0, lines.length).filter(i -> lines[i].contains(find)).boxed().collect(Collectors.toCollection(ArrayList::new)).toArray();
    }

    private static String[] getPackages(String filename) throws FileNotFoundException {
        File java = new File(filename);
        Scanner scanner = new Scanner(java);
        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            if(line.startsWith("package")){
                String[] packages = line.substring(7).split("(\\x2E)");
                packages[0] = packages[0].substring(1);
                packages[packages.length - 1] = packages[packages.length - 1].substring(0, packages[packages.length - 1].length() - 1);
                return packages;
            }
        }
        return new String[]{};
    }

    private static String getClassName(String filename) {
        File java = new File(filename);
        try {
            Scanner scanner = new Scanner(java);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("public class ")) {
                    return line.substring(13, line.length() - 2);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("""
                               ----Help Panel----
                               Usage: cava (mode) (file)
                               
                               Modes:
                                 cpp    Makes files for c++ development based on inputted file
                                 build  Builds the file into a .so & .dll & .dylib
                               ------------------
                               """);
            return;
        }

        if (Objects.equals(args[0], "cpp")) GenerateCpp(args[1]);
        else if (Objects.equals(args[0], "build")){Build(args[1]);}
    }

    private static void GenerateCpp(String file) {
        File source = new File(file);
        if (!source.exists()) System.out.println("File not found");

        // Making Cava directory for generated file
        File dir = new File("Cava");
        if(!dir.exists()) if (!dir.mkdir()) System.out.println("Could not create directory Cava");

        // Running javac on the specified file
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"\"" + System.getenv("JAVA_HOME") + "\\bin\\javac.exe\"", "-d", ".\\Cava","-h", ".\\Cava", "\"" + file + "\""});
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while running javac");
        }

        // Extracting package info from the .java file
        String[] packages = new String[]{};
        try {
            packages = getPackages(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Extracting class name info from the .java file
        String classname = getClassName(file);

        // Extracting functions with the native keyword
        getNativeFunctions(file);

        // Getting the .h file name by adding the packages
        StringBuilder hFileName = new StringBuilder("Cava/");
        for (String pack : packages)
        {
            hFileName.append(pack).append("_");
        }
        hFileName.append(file, 0, file.length() - 4).append("h");

        // Opening the .h file generated by javac
        File hFile = new File(hFileName.toString());
        if(!hFile.exists()) System.out.println("Could not find header file");

        // Writing the correct things to the .h file
        if(hFile.canWrite() && hFile.canRead()){
            try {
                // Read
                Scanner scanner = new Scanner(hFile);
                ArrayList<String> lines = new ArrayList<>();
                while(scanner.hasNextLine()) lines.add(scanner.nextLine());
                scanner.close();

                // Modify
                // TODO find all occurrences of JNIEXPORT and insert a function declaration 2 lines below with c++ types

                lines.add("/// CAVA GENERATION");
                int lineIndex = lines.size();
                StringBuilder indent = new StringBuilder();

                // Adding namespaces
                for (String pack : packages)
                {
                    lines.add(lineIndex, indent + "namespace " + pack);
                    lines.add(lineIndex + 1, "{");
                    lineIndex += 2;
                    lines.add(lineIndex, indent + "}");
                    indent.append("    ");
                }

                // Adding class
                lines.add(lineIndex, indent + "class " + classname);
                lines.add(lineIndex + 1, indent + "{");
                lineIndex += 2;
                lines.add(lineIndex, indent + "}");
                indent.append("    ");

                // Write
                FileWriter writer = new FileWriter(hFile);

                StringBuilder str = new StringBuilder();
                for(String line : lines) str.append(line).append("\n");

                writer.write(str.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private static void Build(String file) {

    }
}
