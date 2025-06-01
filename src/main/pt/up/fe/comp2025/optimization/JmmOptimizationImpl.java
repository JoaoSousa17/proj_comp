package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ConfigOptions;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class JmmOptimizationImpl implements JmmOptimization {

    private static final Logger logger = Logger.getLogger(JmmOptimizationImpl.class.getName());
    private static final int DEFAULT_MAX_ITERATIONS = 5;

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        List<Report> reports = new ArrayList<>();
        try {
            var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
            var ollirCode = visitor.visit(semanticsResult.getRootNode());
            return new OllirResult(semanticsResult, ollirCode, reports);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating OLLIR code", e);
            return new OllirResult(semanticsResult, "", reports);
        }
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // Check if optimization is enabled in configuration
        if (!shouldOptimize(semanticsResult.getConfig())) {
            logger.info("Optimization is disabled by configuration");
            return semanticsResult;
        }

        List<Report> optimizationReports = new ArrayList<>();
        JmmNode root = semanticsResult.getRootNode();

        try {
            // Run the optimization pipeline
            OptimizationResults results = runOptimizationPipeline(root);

            // Add reports from optimization process
            generateOptimizationReports(results, optimizationReports);

            // Log the optimized AST
            logger.fine("Optimized AST:\n" + root.toTree());

            // Combine all reports and return new semantic result
            List<Report> allReports = SpecsCollections.concat(semanticsResult.getReports(), optimizationReports);
            return new JmmSemanticsResult(root, semanticsResult.getSymbolTable(), allReports, semanticsResult.getConfig());

        } catch (Exception e) {
            String errorMsg = "Optimization error: " + e.getMessage();
            logger.log(Level.SEVERE, errorMsg, e);
            optimizationReports.add(new Report(ReportType.ERROR, Stage.OPTIMIZATION, -1, errorMsg));

            // Return result with error reports
            List<Report> allReports = SpecsCollections.concat(semanticsResult.getReports(), optimizationReports);
            return new JmmSemanticsResult(root, semanticsResult.getSymbolTable(), allReports, semanticsResult.getConfig());
        }
    }

    private OptimizationResults runOptimizationPipeline(JmmNode rootNode) {
        OptimizationResults results = new OptimizationResults();
        boolean changesDetected;
        int iteration = 0;

        // Create optimization visitors
        ConstantFoldingVisitor foldingVisitor = new ConstantFoldingVisitor();
        foldingVisitor.enableDebug(false); // Disable internal debug logs

        ConstantPropagationVisitor propagationVisitor = new ConstantPropagationVisitor();

        do {
            iteration++;
            changesDetected = false;

            // Apply constant folding
            foldingVisitor.visit(rootNode);
            int foldingCount = foldingVisitor.getOptimizationCount();

            if (foldingCount > 0) {
                changesDetected = true;
                results.addFoldingCount(foldingCount);
                logger.info("Iteration " + iteration + ": Performed " + foldingCount + " constant folding operations");
            }

            // Create new folding visitor for next iteration
            foldingVisitor = new ConstantFoldingVisitor();

            // Apply constant propagation
            Map<String, String> propagationContext = new HashMap<>();
            boolean propagationResult = propagationVisitor.visit(rootNode, propagationContext);

            if (propagationResult) {
                changesDetected = true;
                results.incrementPropagationIterations();
                logger.info("Iteration " + iteration + ": Constant propagation applied changes");
            }

            // Create new propagation visitor for next iteration
            propagationVisitor = new ConstantPropagationVisitor();

            // Check for max iterations
            if (iteration >= DEFAULT_MAX_ITERATIONS) {
                results.setMaxIterationsReached(true);
                logger.warning("Reached maximum optimization iterations (" + DEFAULT_MAX_ITERATIONS + ")");
                break;
            }

        } while (changesDetected);

        results.setTotalIterations(iteration);
        return results;
    }

    private void generateOptimizationReports(OptimizationResults results, List<Report> reports) {
        // Report if maximum iterations were reached
        if (results.isMaxIterationsReached()) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.OPTIMIZATION,
                    -1,
                    "Optimization reached maximum iterations limit (" + DEFAULT_MAX_ITERATIONS + "); may be incomplete"
            ));
        }

        // Report folding optimizations
        if (results.getTotalFoldings() > 0) {
            reports.add(new Report(
                    ReportType.LOG,
                    Stage.OPTIMIZATION,
                    -1,
                    "Constant Folding: " + results.getTotalFoldings() + " expressions optimized"
            ));
        }

        // Report propagation optimizations
        if (results.getPropagationIterations() > 0) {
            reports.add(new Report(
                    ReportType.LOG,
                    Stage.OPTIMIZATION,
                    -1,
                    "Constant Propagation: changes applied in " + results.getPropagationIterations() + " iterations"
            ));
        }

        // Report total iterations
        reports.add(new Report(
                ReportType.LOG,
                Stage.OPTIMIZATION,
                -1,
                "Optimization process completed after " + results.getTotalIterations() + " iterations"
        ));

        // Report if no optimizations were performed
        if (results.getTotalFoldings() == 0 && results.getPropagationIterations() == 0) {
            reports.add(new Report(
                    ReportType.LOG,
                    Stage.OPTIMIZATION,
                    -1,
                    "No optimizations were applied to the code"
            ));
        }
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        RegisterAllocatorVisitor allocator = new RegisterAllocatorVisitor();
        List<Report> existingReports = new ArrayList<>(ollirResult.getReports());
        int registerSetting = ConfigOptions.getRegisterAllocation(ollirResult.getConfig());

        if (registerSetting != -1) {
            String settingLabel = (registerSetting == 0) ? "minimal (0)" : String.valueOf(registerSetting);
            logger.info("[Register Allocation] Initiating allocation with " + settingLabel + " registers.");

            List<Report> allocationReports = allocator.processMethodRegisters(ollirResult, registerSetting);
            existingReports.addAll(allocationReports);

            if (allocationReports.isEmpty()) {
                logger.info("[Register Allocation] Allocation completed successfully with no reported issues.");
            } else {
                logger.warning("[Register Allocation] Completed with " + allocationReports.size() +
                        " issue(s). Check the report list for further details.");
            }

        }

        logger.fine("[Register Allocation] Skipped: No valid allocation setting provided.");
        return ollirResult;
    }



    private boolean shouldOptimize(Map<String, String> config) {
        return config != null &&
                ("true".equals(config.get("optimize")) ||
                        "true".equals(config.get("-o")) ||
                        config.containsKey("-o"));
    }

    /**
     * Helper class to track optimization statistics
     */
    private static class OptimizationResults {
        private int totalFoldings = 0;
        private int propagationIterations = 0;
        private int totalIterations = 0;
        private boolean maxIterationsReached = false;

        public void addFoldingCount(int count) {
            totalFoldings += count;
        }

        public void incrementPropagationIterations() {
            propagationIterations++;
        }

        public int getTotalFoldings() {
            return totalFoldings;
        }

        public int getPropagationIterations() {
            return propagationIterations;
        }

        public void setTotalIterations(int iterations) {
            this.totalIterations = iterations;
        }

        public int getTotalIterations() {
            return totalIterations;
        }

        public void setMaxIterationsReached(boolean reached) {
            this.maxIterationsReached = reached;
        }

        public boolean isMaxIterationsReached() {
            return maxIterationsReached;
        }
    }
}