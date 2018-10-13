package org.refactoringminer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
			parameters.put("anonymousClass", ref.getAnonymousClass().toString());
			parameters.put("addedClass", ref.getAddedClass().toString());
		} else if (refactoring instanceof ExtractAndMoveOperationRefactoring) {
			ExtractAndMoveOperationRefactoring ref = (ExtractAndMoveOperationRefactoring) refactoring;
			parameters.put("extractedOperation", ref.getExtractedOperation().toString());
			parameters.put("sourceOperationBeforeExtraction", ref.getSourceOperationBeforeExtraction().toString());
			parameters.put("sourceOperationClassBeforeExtraction", ref.getSourceOperationBeforeExtraction().getClassName());
			parameters.put("extractedOperationClass", ref.getExtractedOperation().getClassName());
		} else if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring ref = (ExtractOperationRefactoring) refactoring;
			parameters.put("sourceOperationCodeRangeBeforeExtraction", rangeToMap(ref.getSourceOperationCodeRangeBeforeExtraction()));
			parameters.put("sourceOperationCodeRangeAfterExtraction", rangeToMap(ref.getSourceOperationCodeRangeAfterExtraction()));
			parameters.put("extractedOperationCodeRange", rangeToMap(ref.getExtractedOperationCodeRange()));
			parameters.put("extractedCodeRangeFromSourceOperation", rangeToMap(ref.getExtractedCodeRangeFromSourceOperation()));
			parameters.put("extractedCodeRangeToExtractedOperation", rangeToMap(ref.getExtractedCodeRangeToExtractedOperation()));
			parameters.put("extractedOperationInvocationCodeRange", rangeToMap(ref.getExtractedOperationInvocationCodeRange()));
			parameters.put("extractedOperation", ref.getExtractedOperation().toString());
			parameters.put("sourceOperationBeforeExtraction", ref.getSourceOperationBeforeExtraction().toString());
			parameters.put("sourceOperationClassBeforeExtraction", ref.getSourceOperationBeforeExtraction().getClassName());
			parameters.put("sourceOperationClassAfterExtraction", ref.getSourceOperationAfterExtraction().getClassName());
		} else if (refactoring instanceof ExtractSuperclassRefactoring) {
			ExtractSuperclassRefactoring ref = (ExtractSuperclassRefactoring) refactoring;
			parameters.put("extractedClass", ref.getExtractedClass().toString());
			parameters.put("subclassSet", ref.getSubclassSet());
		} else if (refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring ref = (ExtractVariableRefactoring) refactoring;
			parameters.put("extractedVariableDeclarationCodeRange", rangeToMap(ref.getExtractedVariableDeclarationCodeRange()));
			parameters.put("variableDeclaration", ref.getVariableDeclaration().toString());
			parameters.put("operation", ref.getOperation().toString());
			parameters.put("operationClass", ref.getOperation().getClassName());
		} else if (refactoring instanceof InlineOperationRefactoring) {
			InlineOperationRefactoring ref = (InlineOperationRefactoring) refactoring;
			parameters.put("targetOperationCodeRangeBeforeInline", rangeToMap(ref.getTargetOperationCodeRangeBeforeInline()));
			parameters.put("targetOperationCodeRangeAfterInline", rangeToMap(ref.getTargetOperationCodeRangeAfterInline()));
			parameters.put("inlinedOperationCodeRange", rangeToMap(ref.getInlinedOperationCodeRange()));
			parameters.put("inlinedCodeRangeFromInlinedOperation", rangeToMap(ref.getInlinedCodeRangeFromInlinedOperation()));
			parameters.put("inlinedCodeRangeInTargetOperation", rangeToMap(ref.getInlinedCodeRangeInTargetOperation()));
			parameters.put("inlinedOperationInvocationCodeRange", rangeToMap(ref.getInlinedOperationInvocationCodeRange()));
			parameters.put("inlinedOperation", ref.getInlinedOperation().toString());
			parameters.put("targetOperationAfterInline", ref.getTargetOperationAfterInline().toString());
			parameters.put("targetOperationClassAfterInline", ref.getTargetOperationAfterInline().getClassName());
		} else if (refactoring instanceof MoveAndRenameClassRefactoring) {
			MoveAndRenameClassRefactoring ref = (MoveAndRenameClassRefactoring) refactoring;
			parameters.put("originalClass", ref.getOriginalClassName());
			parameters.put("renamedClass", ref.getRenamedClassName());
		} else if (refactoring instanceof MoveAttributeRefactoring) {
			MoveAttributeRefactoring ref = (MoveAttributeRefactoring) refactoring;
			parameters.put("sourceAttributeCodeRangeBeforeMove", rangeToMap(ref.getSourceAttributeCodeRangeBeforeMove()));
			parameters.put("targetAttributeCodeRangeAfterMove", rangeToMap(ref.getTargetAttributeCodeRangeAfterMove()));
			parameters.put("movedAttribute", ref.getMovedAttribute().toString());
			parameters.put("originalAttributeClass", ref.getOriginalAttribute().getClassName());
			parameters.put("movedAttributeClass", ref.getMovedAttribute().getClassName());
		} else if (refactoring instanceof MoveClassRefactoring) {
			MoveClassRefactoring ref = (MoveClassRefactoring) refactoring;
			parameters.put("originalClass", ref.getOriginalClassName());
			parameters.put("movedClass", ref.getMovedClassName());
		} else if (refactoring instanceof MoveOperationRefactoring) {
			MoveOperationRefactoring ref = (MoveOperationRefactoring) refactoring;
			parameters.put("sourceOperationCodeRangeBeforeMove", rangeToMap(ref.getSourceOperationCodeRangeBeforeMove()));
			parameters.put("targetOperationCodeRangeAfterMove", rangeToMap(ref.getTargetOperationCodeRangeAfterMove()));
			parameters.put("originalOperation", ref.getOriginalOperation().toString());
			parameters.put("originalOperationClass", ref.getOriginalOperation().getClassName());
			parameters.put("movedOperation", ref.getMovedOperation().toString());
			parameters.put("movedOperationClass", ref.getMovedOperation().getClassName());
		} else if (refactoring instanceof MoveSourceFolderRefactoring) {
			MoveSourceFolderRefactoring ref = (MoveSourceFolderRefactoring) refactoring;
			final RenamePattern pattern = ref.getPattern();
			parameters.put("originalPath", pattern.getOriginalPath().endsWith("/") ?
					pattern.getOriginalPath().substring(0, pattern.getOriginalPath().length() - 1) :
					pattern.getOriginalPath());
			parameters.put("movedPath", pattern.getMovedPath().endsWith("/") ?
					pattern.getMovedPath().substring(0, pattern.getMovedPath().length() - 1) :
					pattern.getMovedPath());
		} else if (refactoring instanceof RenameAttributeRefactoring) {
			RenameAttributeRefactoring ref = (RenameAttributeRefactoring) refactoring;
			parameters.put("originalAttribute", ref.getOriginalAttribute().toString());
			parameters.put("renamedAttribute", ref.getRenamedAttribute());
			parameters.put("classNameAfter", ref.getClassNameAfter());
		} else if (refactoring instanceof RenameClassRefactoring) {
			RenameClassRefactoring ref = (RenameClassRefactoring) refactoring;
			parameters.put("originalClass", ref.getOriginalClassName());
			parameters.put("renamedClass", ref.getRenamedClassName());
		} else if (refactoring instanceof RenameOperationRefactoring) {
			RenameOperationRefactoring ref = (RenameOperationRefactoring) refactoring;
			parameters.put("sourceOperationCodeRangeBeforeRename", rangeToMap(ref.getSourceOperationCodeRangeBeforeRename()));
			parameters.put("targetOperationCodeRangeAfterRename", rangeToMap(ref.getTargetOperationCodeRangeAfterRename()));
			parameters.put("originalOperation", ref.getOriginalOperation().toString());
			parameters.put("renamedOperation", ref.getRenamedOperation().toString());
			parameters.put("originalOperationClass", ref.getOriginalOperation().getClassName());
			parameters.put("renamedOperationClass", ref.getRenamedOperation().getClassName());
		} else if (refactoring instanceof RenamePackageRefactoring) {
			RenamePackageRefactoring ref = (RenamePackageRefactoring) refactoring;
			final RenamePattern pattern = ref.getPattern();
			parameters.put("originalPath", pattern.getOriginalPath().endsWith(".") ?
					pattern.getOriginalPath().substring(0, pattern.getOriginalPath().length() - 1) :
					pattern.getOriginalPath());
			parameters.put("movedPath", pattern.getMovedPath().endsWith(".") ?
					pattern.getMovedPath().substring(0, pattern.getMovedPath().length() - 1) :
					pattern.getMovedPath());
		} else if (refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring ref = (RenameVariableRefactoring) refactoring;
			parameters.put("originalVariable", ref.getOriginalVariable().toString());
			parameters.put("renamedVariable", ref.getRenamedVariable().toString());
			parameters.put("operationAfter", ref.getOperationAfter().toString());
			parameters.put("operationAfterClass", ref.getOperationAfter().getClassName());
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

}
