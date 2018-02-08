

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class ResultFormatter {
	private final List<String[]>	rows	= new LinkedList<String[]>();

	
	public void addRow(final String... cols) {
		this.rows.add(cols);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		final int[] columWidth = this.colWidths();

		for (final String[] row : this.rows) {
			for (int colNum = 0; colNum < row.length; colNum++) {
				builder.append(StringUtils.rightPad(StringUtils.defaultString(row[colNum]), columWidth[colNum]));
				builder.append('\t');
			}

			builder.append('\n');
		}
		return builder.toString();
	}

	
	private int[] colWidths() {
		int cols = -1;
		for (final String[] row : this.rows) {
			cols = Math.max(cols, row.length);
		}

		final int[] widths = new int[cols];
		for (final String[] row : this.rows) {
			for (int colNum = 0; colNum < row.length; colNum++) {
				widths[colNum] = Math.max(widths[colNum], StringUtils.length(row[colNum]));
			}
		}

		return widths;
	}
}



