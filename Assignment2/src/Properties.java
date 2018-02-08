
import java.util.HashMap;
import java.util.Map;

public class Properties {
	private int								docFreq;
	private Map<String, DocumentProperty>	postingFile	= new HashMap<>();
	private Map<String, Integer>			termFreq	= new HashMap<>();

	
	public final Map<String, Integer> getTermFreq() {
		return this.termFreq;
	}

	    
	public final void setTermFreq(final Map<String, Integer> termFreq) {
		this.termFreq = termFreq;
	}

	
	public final int getDocFreq() {
		return this.docFreq;
	}

	
	public final void setDocFreq(final int docFreq) {
		this.docFreq = docFreq;
	}

	
	public final Map<String, DocumentProperty> getPostingFile() {
		return this.postingFile;
	}

	
	public final void setPostingFile(final Map<String, DocumentProperty> postingFile) {
		this.postingFile = postingFile;
	}

}

