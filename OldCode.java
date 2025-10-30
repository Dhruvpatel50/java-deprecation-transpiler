import java.util.*;
import java.net.URL;

public class OldCode implements Runnable {
    @Override
    public void run() {
        System.out.println("--- Executing the test code ---");

        // Deprecated Type: StringBuffer -> StringBuilder
        StringBuffer buffer = new StringBuffer();
        buffer.append("hello");
        System.out.println("Mutable string says: " + buffer.toString());

        // Deprecated Method: Date.getYear()
        Date today = new Date();
        int year = today.getYear() + 1900;
        System.out.println("The year is: " + year);

        // Deprecated Constructor: new URL(String)
        try {
            URL myUrl = new URL("https://www.google.com");
            System.out.println("URL Host: " + myUrl.getHost());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Deprecated Type: StringTokenizer -> String[]
        String data = "java is fun";
        StringTokenizer tokenizer = new StringTokenizer(data);
        // This line is tricky, so our test will just check the new array
        System.out.println("Tokenizer has been replaced.");
    }
}


// `CompilationUnit`
// ├── `ImportDeclaration` (name: `java.util.*`)
// └── `ClassOrInterfaceDeclaration` (modifiers: `public`, name: `OldCode`, implementedType: `Runnable`)
//     └── `MethodDeclaration` (modifiers: `public`, name: `run`, overrideAnnotation: `@Override`)
//         ├── `BlockStmt` (Body of the `run` method)
//             ├── `ExpressionStmt`
//             │   └── `MethodCallExpr` (scope: `System.out`, name: `println`, arguments: [`StringLiteralExpr`("--- Executing the test code ---")])

//             ├── `VariableDeclarationExpr`
//             │   └── `VariableDeclarator` (name: `buffer`, type: `ClassOrInterfaceType`("StringBuffer"), initializer: `ObjectCreationExpr` (type: `ClassOrInterfaceType`("StringBuffer")))
//             ├── `ExpressionStmt`
//             │   └── `MethodCallExpr` (scope: `buffer`, name: `append`, arguments: [`StringLiteralExpr`("hello")])
//             ├── `ExpressionStmt`
//             │   └── `MethodCallExpr` (scope: `System.out`, name: `println`, arguments: [`BinaryExpr` (left: `StringLiteralExpr`("Mutable string says: "), operator: `PLUS`, right: `MethodCallExpr` (scope: `buffer`, name: `toString`))])

//             ├── `VariableDeclarationExpr`
//             │   └── `VariableDeclarator` (name: `today`, type: `ClassOrInterfaceType`("Date"), initializer: `ObjectCreationExpr` (type: `ClassOrInterfaceType`("Date")))
//             ├── `VariableDeclarationExpr`
//             │   └── `VariableDeclarator` (name: `year`, type: `PrimitiveType`("int"), initializer: `BinaryExpr` (left: `MethodCallExpr` (scope: `today`, name: `getYear`), operator: `PLUS`, right: `IntegerLiteralExpr`("1900")))
//             ├── `ExpressionStmt`
//             │   └── `MethodCallExpr` (scope: `System.out`, name: `println`, arguments: [`BinaryExpr` (left: `StringLiteralExpr`("The year is: "), operator: `PLUS`, right: `NameExpr`("year"))])
