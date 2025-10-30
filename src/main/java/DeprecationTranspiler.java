import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.io.InputStream;

public class DeprecationTranspiler {

    private static final class DeprecationRule {
        public final String description;
        public final Class<? extends Node> nodeType;
        public final Function<Node, Boolean> isDeprecated;
        public final Function<Node, Visitable> getReplacement;

        public DeprecationRule(String description, Class<? extends Node> nodeType, Function<Node, Boolean> isDeprecated, Function<Node, Visitable> getReplacement) {
            this.description = description;
            this.nodeType = nodeType;
            this.isDeprecated = isDeprecated;
            this.getReplacement = getReplacement;
        }
    }

    private static final List<DeprecationRule> rules = new ArrayList<>();

    static {
        // Load rules from resources/rules.json
        try (InputStream is = DeprecationTranspiler.class.getResourceAsStream("/rules.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> raw = mapper.readValue(is, new TypeReference<List<Map<String, Object>>>(){});
                for (Map<String, Object> r : raw) {
                    String kind = (String) r.get("kind");
                    String match = (String) r.get("match");
                    String desc = (String) r.getOrDefault("description", "");
                    String replacementExpr = (String) r.get("replacementExpression");
                    String replacementType = (String) r.get("replacementType");
                    Object importsObj = r.get("imports");
                    List<String> imports = new ArrayList<>();
                    if (importsObj instanceof List) {
                        for (Object o : (List<?>) importsObj) {
                            if (o != null) imports.add(o.toString());
                        }
                    }

                    if ("MethodCall".equals(kind)) {
                        rules.add(new DeprecationRule(desc, MethodCallExpr.class,
                                node -> ((MethodCallExpr) node).getNameAsString().equals(match),
                                node -> {
                                    MethodCallExpr m = (MethodCallExpr) node;
                                    m.findCompilationUnit().ifPresent(cu -> imports.forEach(cu::addImport));
                                    String target = m.getScope().orElseThrow().toString();
                                    String expr = String.format(replacementExpr, target);
                                    return StaticJavaParser.parseExpression(expr);
                                }
                        ));
                    } else if ("ObjectCreation".equals(kind)) {
                        rules.add(new DeprecationRule(desc, ObjectCreationExpr.class,
                                node -> {
                                    ObjectCreationExpr oce = (ObjectCreationExpr) node;
                                    return oce.getType().getNameAsString().equals(match) && oce.getArguments().size() == 1 && !isVariableDeclaration(node);
                                },
                                node -> {
                                    ObjectCreationExpr oce = (ObjectCreationExpr) node;
                                    oce.findCompilationUnit().ifPresent(cu -> imports.forEach(cu::addImport));
                                    String arg = oce.getArgument(0).toString();
                                    String expr = String.format(replacementExpr, arg);
                                    return StaticJavaParser.parseExpression(expr);
                                }
                        ));
                    } else if ("VariableType".equals(kind)) {
                        rules.add(new DeprecationRule(desc, VariableDeclarator.class,
                                node -> ((VariableDeclarator) node).getType().asString().equals(match),
                                node -> {
                                    VariableDeclarator vd = (VariableDeclarator) node;
                                    vd.setType(replacementType);
                                    // try to fix initializer if it's an ObjectCreation
                                    vd.getInitializer().ifPresent(init -> {
                                        if (init instanceof ObjectCreationExpr) {
                                            ObjectCreationExpr oce = (ObjectCreationExpr) init;
                                            if (oce.getType().getNameAsString().equals(match)) {
                                                oce.setType(replacementType);
                                            }
                                        }
                                    });
                                    return vd;
                                }
                        ));
                    }
                }
            } else {
                System.err.println("rules.json not found in resources; using built-in rules (none).");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rules.json", e);
        }
    }

    private static boolean isVariableDeclaration(Node node) {
        return node.getParentNode().map(p -> p instanceof VariableDeclarator).orElse(false);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java -jar your-jar-file.jar <path-to-java-file>");
            return;
        }
        File sourceFile = new File(args[0]);
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        //lexical analysis and syntax analysis
        CompilationUnit cu = StaticJavaParser.parse(sourceCode); //AST
        Optional<String> mainClassName = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString);

        if (mainClassName.isEmpty()) {
            System.err.println("Could not find a class declaration.");
            return;
        }

        System.out.println("🔍 Analyzing " + sourceFile.getName() + "...");
        DeprecationVisitor visitor = new DeprecationVisitor(rules);
        cu.accept(visitor, null);

        if (visitor.wasChangeMade()) {
            String newCode = cu.toString();
            System.out.println("\n Code transformation complete. The final code is:\n");
            System.out.println("------------------------------------");
            System.out.println(newCode);
            System.out.println("------------------------------------");
            System.out.println("\n Now, compiling and executing the updated code...\n");
            DynamicJavaCompiler.compileAndRun(mainClassName.get(), newCode);
        } else {
            System.out.println("No deprecated APIs found. Code is modern.");
            System.out.println("\n Executing original code...\n");
            DynamicJavaCompiler.compileAndRun(mainClassName.get(), sourceCode);
        }
    }

    private static class DeprecationVisitor extends ModifierVisitor<Void> {
        private final List<DeprecationRule> rules;
        private boolean changeMade = false;
        private static final Map<String, String> typeReplacements = new HashMap<>();
        
        static {
            typeReplacements.put("StringBuffer", "StringBuilder");
            typeReplacements.put("StringTokenizer", "String[]");
        }

        public DeprecationVisitor(List<DeprecationRule> rules) {
            this.rules = rules;
        }

        @Override
        public Visitable visit(VariableDeclarator n, Void arg) {
            super.visit(n, arg);
            
            String varType = n.getType().asString();
            
            if (typeReplacements.containsKey(varType)) {
                System.out.println("\n WARNING: Found deprecated type '" + varType + "' for variable '" + n.getNameAsString() + "' on line " + n.getRange().orElseThrow().begin.line);
                
                n.setType(typeReplacements.get(varType));
                
                n.getInitializer().ifPresent(initializer -> {
                    if (initializer instanceof ObjectCreationExpr) {
                        ObjectCreationExpr oce = (ObjectCreationExpr) initializer;
                        String creationType = oce.getType().getNameAsString();
                        if (creationType.equals("Vector")) oce.setType("ArrayList");
                        
                        // *** THIS IS THE FIX ***
                        if (creationType.equals("StringBuffer")) oce.setType("StringBuilder");
                    }
                });
                
                if (varType.equals("StringTokenizer") && n.getInitializer().isPresent()) {
                    ObjectCreationExpr oce = (ObjectCreationExpr) n.getInitializer().get();
                    String sourceString = oce.getArgument(0).toString();
                    String delimiter = (oce.getArguments().size() > 1) ? oce.getArgument(1).toString() : "\" \"";
                    n.setInitializer(String.format("%s.split(%s)", sourceString, delimiter));
                }
                
                changeMade = true;
            }
            return n;
        }

        private Visitable applyRules(Node node) {
            for (DeprecationRule rule : rules) {
                if (rule.nodeType.isInstance(node) && rule.isDeprecated.apply(node)) {
                    System.out.println("\n WARNING: Found " + rule.description + " on line " + node.getRange().orElseThrow().begin.line);
                    changeMade = true;
                    return rule.getReplacement.apply(node);
                }
            }
            return node;
        }

        @Override public Visitable visit(ObjectCreationExpr n, Void arg) { super.visit(n, arg); return applyRules(n); }
        @Override public Visitable visit(MethodCallExpr n, Void arg) { super.visit(n, arg); return applyRules(n); }
        public boolean wasChangeMade() { return changeMade; }
    }
}