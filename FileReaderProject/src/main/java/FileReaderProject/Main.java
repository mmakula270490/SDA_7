package FileReaderProject;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Reader {

	List<String> headers = new ArrayList<String>();
	List<String> headersTypes = new ArrayList<String>();
	String data = "";

	public void read(String fileName) {

		try {

			FileInputStream excelFile = new FileInputStream(new File(fileName));
			Workbook workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = datatypeSheet.iterator();

			while (iterator.hasNext()) {

				Row currentRow = iterator.next();
				Iterator<Cell> cellIterator = currentRow.iterator();

				while (cellIterator.hasNext()) {

					Cell currentCell = cellIterator.next();

					if (currentCell.getRowIndex() < 2) {
						if (currentCell.getRowIndex() == 0) {
							headers.add(currentCell.getStringCellValue());
						}
						if (currentCell.getRowIndex() == 1) {
							headersTypes.add(currentCell.getStringCellValue());
						}
					} else {

						if (currentCell.getColumnIndex() == 0) {
							data += "(";
						}

						if (currentCell.getCellType() == CellType.STRING) {
							data += ("\'" + currentCell.getStringCellValue() + "'");
						} else if (currentCell.getCellType() == CellType.NUMERIC) {
							data += (String.valueOf((int) currentCell.getNumericCellValue()));
						}
						if (currentCell.getColumnIndex() > 0
								&& currentCell.getColumnIndex() == currentRow.getPhysicalNumberOfCells() - 1) {
							data += ")";
						}
						if (currentCell.getColumnIndex() < currentRow.getPhysicalNumberOfCells()) {
							data += ",";
						}
					}
				}

			}
			excelFile.close();
			workbook.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (data.endsWith(",")) {
			data = data.substring(0, data.length() - 1) + ";";
		}

	}

	public List<String> getHeaders() {
		return headers;
	}

	public List<String> getHeadersTypes() {
		return headersTypes;
	}

	public String getData() {
		return data;
	}
}

class DatabaseHandler {

	Connection connection;
	Statement statement;

	public DatabaseHandler() throws SQLException {

		try {
			Class.forName("org.postgresql.Driver");

			connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/fileReaderDB", "postgres", "270490");
			statement = connection.createStatement();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void createTable(String tableName, List<String> header, List<String> headersTypes) throws SQLException {

		statement.execute("CREATE TABLE " + tableName + "( " + headersAndTypesGenerator(header, headersTypes) + ");");
	}

	public void insertData(String tableName, List<String> header, String data) throws SQLException {

		statement.execute("INSERT INTO " + tableName + "( " + headersGenerator(header) + ") VALUES " + data);
	}

	public String headersGenerator(List<String> header) {
		String columns = "";

		for (int i = 0; i < header.size(); i++) {
			if (i != header.size() - 1)
				columns += header.get(i) + ", ";
			else
				columns += header.get(i);
		}
		return columns;
	}

	public String headersAndTypesGenerator(List<String> header, List<String> headersTypes) {
		String columns = "";

		for (int i = 0; i < header.size(); i++) {
			if (i != header.size() - 1)
				columns += header.get(i) + " " + headersTypes.get(i) + ", ";
			else
				columns += header.get(i) + " " + headersTypes.get(i);
		}
		return columns;
	}
}

public class Main {

	public static void main(String[] args) throws SQLException {

		String fileName = "test";
		String fileExtension = ".xlsx";

		DatabaseHandler databaseHandler = new DatabaseHandler();
		Reader fileReader = new Reader();

		fileReader.read(fileName + fileExtension);
		databaseHandler.createTable(fileName, fileReader.getHeaders(), fileReader.getHeadersTypes());
		databaseHandler.insertData(fileName, fileReader.getHeaders(), fileReader.getData());
		
		databaseHandler.statement.close();
		databaseHandler.connection.close();
	}
}
