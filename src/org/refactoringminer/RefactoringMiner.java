package org.refactoringminer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class RefactoringMiner {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw argumentException();
		}

		final String option = args[0];
		if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--h") || option.equalsIgnoreCase("-help")
				|| option.equalsIgnoreCase("--help")) {
			printTips();
			return;
		}

		if (option.equalsIgnoreCase("-a")) {
			detectAll(args);
		} else if (option.equalsIgnoreCase("-bc")) {
			detectBetweenCommits(args);
		} else if (option.equalsIgnoreCase("-bt")) {
			detectBetweenTags(args);
		} else if (option.equalsIgnoreCase("-c")) {
			detectAtCommit(args);
		} else if (option.equalsIgnoreCase("-al")) {
			detectAllWithLocationInformation(args);
		} else {
			throw argumentException();
		}
	}

	private static void detectAll(String[] args) throws Exception {
		if (args.length > 3) {
			throw argumentException();
		}
		String folder = args[1];
		String branch = null;
		if (args.length == 3) {
			branch = args[2];
		}
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = (branch == null) ? "all_refactorings.csv" : "all_refactorings_" + branch + ".csv";
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectAll(repo, branch, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);

						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectBetweenCommits(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startCommit = args[2];
		String endCommit = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = null;
			if (endCommit == null) {
				fileName = "refactorings_" + startCommit + "_begin" + ".csv";
			} else {
				fileName = "refactorings_" + startCommit + "_" + endCommit + ".csv";
			}
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);
						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectBetweenTags(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startTag = args[2];
		String endTag = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = null;
			if (endTag == null) {
				fileName = "refactorings_" + startTag + "_begin" + ".csv";
			} else {
				fileName = "refactorings_" + startTag + "_" + endTag + ".csv";
			}
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectBetweenTags(repo, startTag, endTag, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);
						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectAtCommit(String[] args) throws Exception {
		if (args.length != 3) {
			throw argumentException();
		}
		String folder = args[1];
		String commitId = args[2];
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectAtCommit(repo, null, commitId, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId + ": ");
						for (Refactoring ref : refactorings) {
							System.out.println("  " + ref);
						}
					}
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void printTips() {
		System.out.println("-h\t\t\t\t\t\t\t\tShow tips");
		System.out.println(
				"-a <git-repo-folder> <branch>\t\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
		System.out.println(
				"-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>\tDetect refactorings Between <star-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"-bt <git-repo-folder> <start-tag> <end-tag>\t\t\tDetect refactorings Between <start-tag> and <end-tag> for project <git-repo-folder>");
		System.out.println(
				"-c <git-repo-folder> <commit-sha1>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"-al <git-repo-folder> <branch>\t\t\t\t\tDetect all refactorings with location information at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
	}

	private static void detectAllWithLocationInformation(String[] args) throws Exception {
		if (args.length > 3) {
			throw argumentException();
		}
		String folder = args[1];
		String branch = null;
		if (args.length == 3) {
			branch = args[2];
		}
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = (branch == null) ? "all_refactorings.json" : "all_refactorings_" + branch + ".json";
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			ObjectMapper mapper = new ObjectMapper();
			detector.detectAll(repo, branch, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);

						for (Refactoring ref : refactorings) {
							Map<String, Object> info = new LinkedHashMap<>();
							info.put("commitId", commitId);
							info.put("name", ref.getName());
							info.put("description", ref.toString());
							info.put("parameters", getParameters(ref));
							try {
								saveToFile(filePath, mapper.writeValueAsString(info));
							} catch (JsonProcessingException e) {
								System.err.println("Error processing save refactorings to file");
								e.printStackTrace(System.err);
							}
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static IllegalArgumentException argumentException() {
		return new IllegalArgumentException("Type `RefactoringMiner -h` to show usage.");
	}

	private static String getResultRefactoringDescription(String commitId, Refactoring ref) {
		StringBuilder builder = new StringBuilder();
		builder.append(commitId);
		builder.append(";");
		builder.append(ref.getName());
		builder.append(";");
		builder.append(ref);
		return builder.toString();
	}

	private static void saveToFile(String fileName, String content) {
		Path path = Paths.get(fileName);
		byte[] contentBytes = (content + System.lineSeparator()).getBytes();
		try {
			Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getResultHeader() {
		return "CommitId;RefactoringType;RefactoringDetail";
	}

	private static Map<String, Object> getParameters(Refactoring refactoring) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		if (refactoring instanceof ConvertAnonymousClassToTypeRefactoring) {
			ConvertAnonymousClassToTypeRefactoring ref = (ConvertAnonymousClassToTypeRefactoring) refactoring;
			parameters.put("anonymousClass", classToMap(ref.getAnonymousClass()));
			parameters.put("addedClass", classToMap(ref.getAddedClass()));
		} else if (refactoring instanceof ExtractAndMoveOperationRefactoring) {
			ExtractAndMoveOperationRefactoring ref = (ExtractAndMoveOperationRefactoring) refactoring;
			parameters.put("extractedOperation", operationToMap(ref.getExtractedOperation()));
			parameters.put("sourceOperationBeforeExtraction", operationToMap(ref.getSourceOperationBeforeExtraction()));
			parameters.put("sourceOperationAfterExtraction", operationToMap(ref.getSourceOperationAfterExtraction()));
		} else if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring ref = (ExtractOperationRefactoring) refactoring;
			Map<String, Object> extractedOperation = operationToMap(ref.getExtractedOperation());
			extractedOperation.put("fragments", rangeToMap(ref.getExtractedCodeRangeToExtractedOperation()));
			parameters.put("extractedOperation", extractedOperation);
			Map<String, Object> sourceOperationBeforeExtraction = operationToMap(ref.getSourceOperationBeforeExtraction());
			sourceOperationBeforeExtraction.put("fragments", rangeToMap(ref.getExtractedCodeRangeFromSourceOperation()));
			parameters.put("sourceOperationBeforeExtraction", sourceOperationBeforeExtraction);
			parameters.put("sourceOperationAfterExtraction", operationToMap(ref.getSourceOperationAfterExtraction()));
			parameters.put("extractedOperationInvocation", operationInvocationToMap(ref.getExtractedOperationInvocation()));
		} else if (refactoring instanceof ExtractSuperclassRefactoring) {
			ExtractSuperclassRefactoring ref = (ExtractSuperclassRefactoring) refactoring;
			parameters.put("extractedClass", classToMap(ref.getExtractedClass()));
			parameters.put("subclassSet", ref.getSubclassUMLSet().stream().map(RefactoringMiner::classToMap).collect(Collectors.toSet()));
		} else if (refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring ref = (ExtractVariableRefactoring) refactoring;
			parameters.put("variableDeclaration", variableDeclarationToMap(ref.getVariableDeclaration()));
			parameters.put("operation", operationToMap(ref.getOperation()));
		} else if (refactoring instanceof InlineOperationRefactoring) {
			InlineOperationRefactoring ref = (InlineOperationRefactoring) refactoring;
			Map<String, Object> inlinedOperation = operationToMap(ref.getInlinedOperation());
			inlinedOperation.put("fragments", rangeToMap(ref.getInlinedCodeRangeFromInlinedOperation()));
			parameters.put("inlinedOperation", inlinedOperation);
			Map<String, Object> targetOperationAfterInline =  operationToMap(ref.getTargetOperationAfterInline());
			targetOperationAfterInline.put("fragments", rangeToMap(ref.getInlinedCodeRangeInTargetOperation()));
			parameters.put("targetOperationAfterInline", targetOperationAfterInline);
			parameters.put("targetOperationBeforeInline", operationToMap(ref.getTargetOperationBeforeInline()));
			parameters.put("inlinedOperationInvocation", operationInvocationToMap(ref.getInlinedOperationInvocation()));
		} else if (refactoring instanceof MoveAndRenameClassRefactoring) {
			MoveAndRenameClassRefactoring ref = (MoveAndRenameClassRefactoring) refactoring;
			parameters.put("originalClass", classToMap(ref.getOriginalClass()));
			parameters.put("renamedClass", classToMap(ref.getRenamedClass()));
		} else if (refactoring instanceof MoveAttributeRefactoring) {
			MoveAttributeRefactoring ref = (MoveAttributeRefactoring) refactoring;
			parameters.put("originalAttribute", attributeToMap(ref.getOriginalAttribute()));
			parameters.put("movedAttribute", attributeToMap(ref.getMovedAttribute()));
		} else if (refactoring instanceof MoveClassRefactoring) {
			MoveClassRefactoring ref = (MoveClassRefactoring) refactoring;
			parameters.put("originalClass", classToMap(ref.getOriginalClass()));
			parameters.put("movedClass", classToMap(ref.getMovedClass()));
		} else if (refactoring instanceof MoveOperationRefactoring) {
			MoveOperationRefactoring ref = (MoveOperationRefactoring) refactoring;
			parameters.put("originalOperation", operationToMap(ref.getOriginalOperation()));
			parameters.put("movedOperation", operationToMap(ref.getMovedOperation()));
		} else if (refactoring instanceof MoveSourceFolderRefactoring) {
			MoveSourceFolderRefactoring ref = (MoveSourceFolderRefactoring) refactoring;
			parameters.put("originalPath", removeIfEndWith(ref.getPattern().getBefore(), "/"));
			parameters.put("movedPath", removeIfEndWith(ref.getPattern().getAfter(), "/"));
		} else if (refactoring instanceof RenameAttributeRefactoring) {
			RenameAttributeRefactoring ref = (RenameAttributeRefactoring) refactoring;
			parameters.put("originalAttribute", variableDeclarationToMap(ref.getOriginalAttribute()));
			parameters.put("renamedAttribute", variableDeclarationToMap(ref.getRenamedAttribute()));
		} else if (refactoring instanceof RenameClassRefactoring) {
			RenameClassRefactoring ref = (RenameClassRefactoring) refactoring;
			parameters.put("originalClass", classToMap(ref.getOriginalClass()));
			parameters.put("renamedClass", classToMap(ref.getRenamedClass()));
		} else if (refactoring instanceof RenameOperationRefactoring) {
			RenameOperationRefactoring ref = (RenameOperationRefactoring) refactoring;
			parameters.put("originalOperation", operationToMap(ref.getOriginalOperation()));
			parameters.put("renamedOperation", operationToMap(ref.getRenamedOperation()));
		} else if (refactoring instanceof RenamePackageRefactoring) {
			RenamePackageRefactoring ref = (RenamePackageRefactoring) refactoring;
			parameters.put("originalPath", removeIfEndWith(ref.getPattern().getBefore(), "."));
			parameters.put("movedPath", removeIfEndWith(ref.getPattern().getAfter(), "."));
		} else if (refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring ref = (RenameVariableRefactoring) refactoring;
			parameters.put("originalVariable", variableDeclarationToMap(ref.getOriginalVariable()));
			parameters.put("renamedVariable", variableDeclarationToMap(ref.getRenamedVariable()));
			parameters.put("operationBefore", operationToMap(ref.getOperationBefore()));
			parameters.put("operationAfter", operationToMap(ref.getOperationAfter()));
		}
		return parameters;
	}

	private static Map<String, Integer> rangeToMap(CodeRange range) {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("startLine", range.getStartLine());
		map.put("endLine", range.getEndLine());
		map.put("startColumn", range.getStartColumn());
		map.put("endColumn", range.getEndColumn());
		return map;
	}

	private static Map<String, Object> operationToMap(UMLOperation op) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("string", op.toString());
		map.put("class", op.getClassName());
		map.put("file", op.getLocationInfo().getFilePath());
		map.put("range", rangeToMap(op.codeRange()));
		return map;
	}

	private static Map<String, Object> classToMap(UMLAbstractClass c) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("string", c.toString());
		map.put("file", c.getSourceFile());
		return map;
	}

	private static Map<String, Object> attributeToMap(UMLAttribute attr) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("string", attr.toString());
		map.put("class", attr.getClassName());
		map.put("file", attr.getLocationInfo().getFilePath());
		map.put("range", rangeToMap(attr.codeRange()));
		return map;
	}

	private static Map<String, Object> operationInvocationToMap(OperationInvocation oi) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("string", oi.toString());
		map.put("file", oi.getLocationInfo().getFilePath());
		map.put("range", rangeToMap(oi.codeRange()));
		return map;
	}

	private static Map<String, Object> variableDeclarationToMap(VariableDeclaration vd) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("string", vd.toString());
		map.put("file", vd.getLocationInfo().getFilePath());
		map.put("range", rangeToMap(vd.codeRange()));
		return map;
	}

	private static String removeIfEndWith(String string, String suffix) {
		return string.endsWith(suffix) ? string.substring(0, string.length() - 1) : string;
	}

}
