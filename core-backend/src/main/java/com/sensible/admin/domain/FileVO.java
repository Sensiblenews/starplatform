package com.sensible.admin.domain;

import java.io.Serializable;

public class FileVO implements Serializable{

	private static final long serialVersionUID = -4316486264956011612L;
	
	private Integer fileSeq;
	private Integer fileNo;
	private String origFileNm;		/* 원본 파일 이름 */
	private String storedFileNm;	/* 저장된 파일 이름 */
	private String filePath;		/* 파일 경로 */
	private Long fileSize;			/* 파일 크기(사이즈) */
	private String fileType;
	private String fileResult;
	private String fileExe;
	
	/* 첨부파일 */
	private Long[] fileSizes;		// 파일 사이즈
	private String[] origFileNms;	// 원본 파일 이름
	private String[] storedFileNms;	// 저장된 파일 이름
	private String[] filePaths;		// 파일 경로
	
	public Integer getFileSeq() {
		return fileSeq;
	}
	public void setFileSeq(Integer fileSeq) {
		this.fileSeq = fileSeq;
	}
	public Integer getFileNo() {
		return fileNo;
	}
	public void setFileNo(Integer fileNo) {
		this.fileNo = fileNo;
	}
	public String getOrigFileNm() {
		return origFileNm;
	}
	public void setOrigFileNm(String origFileNm) {
		this.origFileNm = origFileNm;
	}
	public String getStoredFileNm() {
		return storedFileNm;
	}
	public void setStoredFileNm(String storedFileNm) {
		this.storedFileNm = storedFileNm;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public String getFileResult() {
		return fileResult;
	}
	public void setFileResult(String fileResult) {
		this.fileResult = fileResult;
	}
	public String getFileExe() {
		return fileExe;
	}
	public void setFileExe(String fileExe) {
		this.fileExe = fileExe;
	}
	public Long[] getFileSizes() {
		if (fileSizes == null) {
			return null;
		}
		Long[] fileSizesNew = new Long[fileSizes.length];
	    System.arraycopy(fileSizes, 0, fileSizesNew, 0, fileSizes.length);

		return fileSizesNew;
		
		//return fileSizes;
	}
	public void setFileSizes(Long[] fileSizesNew) {
		
		if (fileSizesNew == null) {
			return;
		}
	    this.fileSizes = new Long[fileSizesNew.length];
	    System.arraycopy(fileSizesNew, 0, fileSizes, 0, fileSizesNew.length);
	    
	  //this.fileSizes = fileSizes;
	}
	public String[] getOrigFileNms() {
		if (origFileNms == null) {
			return null;
		}
		
		String[] origFileNmsNew = new String[origFileNms.length];
	    System.arraycopy(origFileNms, 0, origFileNmsNew, 0, origFileNms.length);
	    return origFileNmsNew;
	    
		//return origFileNms;
	}
	public void setOrigFileNms(String[] origFileNmsNew) {
	    if (origFileNmsNew == null) {
	    	return;
	    }
		this.origFileNms = new String[origFileNmsNew.length];
		System.arraycopy(origFileNmsNew, 0, origFileNms, 0, origFileNmsNew.length);
	    
		//this.origFileNms = origFileNms;
	}
	public String[] getStoredFileNms() {
		if (storedFileNms == null) {
			return null;
		}
		String[] storedFileNmsNew = new String[storedFileNms.length];
	    System.arraycopy(storedFileNms, 0, storedFileNmsNew, 0, storedFileNms.length);
	    return storedFileNmsNew;
	    
		//return storedFileNms;
	}
	public void setStoredFileNms(String[] storedFileNmsNew) {
		if (storedFileNmsNew == null) {
			return;
		}
		this.storedFileNms = new String[storedFileNmsNew.length];
		System.arraycopy(storedFileNmsNew, 0, storedFileNms, 0, storedFileNmsNew.length);
		    
		//this.storedFileNms = storedFileNms;
	}
	public String[] getFilePaths() {
		 
		if (filePaths == null) {
			return null;
		}
		 
		String[] filePathsNew = new String[filePaths.length];
	    System.arraycopy(filePaths, 0, filePathsNew, 0, filePaths.length);
	    return filePathsNew;
	    
		 //return filePaths;
	}
	public void setFilePaths(String[] filePathsNew) {
		 if (filePathsNew == null) {
			 return;
		 }
		 this.filePaths = new String[filePathsNew.length];
		 System.arraycopy(filePathsNew, 0, filePaths, 0, filePathsNew.length);
		    
	//	this.filePaths = filePaths;
	}
	
}
