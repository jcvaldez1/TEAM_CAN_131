import java.util.ArrayList;
import Jama.*;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;

public class PvmmCurveFitter{
	CsvToMatrixParser ctmp;
	ArrayList<String[]> parseList;

	Matrix closingPriceVector;
	Matrix previousPriceVector;
	Matrix previousVolumeVector;
	Matrix momentum22Vector;
	Matrix momentum7Vector;

	Matrix finalMatrix;

	public static void main(String args[]){
		PvmmCurveFitter pcf = new PvmmCurveFitter();
		pcf.theRealMain();
	}

	public void theRealMain(){
		ctmp = new CsvToMatrixParser();
		parseList = ctmp.parseCsvToList("ap_data.csv");
		initVectorsAndMatrices();
		printFinalMatrix();
	}

	public void initVectorsAndMatrices(){
		closingPriceVector = getClosingPrice();
		previousPriceVector = getPreviousPrice();
		previousVolumeVector = getPreviousVolume();
		momentum22Vector = getMomentum(22);
		momentum7Vector = getMomentum(7);

		Matrix concat = concatLR(closingPriceVector, previousPriceVector);
		concat = concatLR(concat, previousVolumeVector);
		concat = concatLR(concat, momentum22Vector);
		finalMatrix = concatLR(concat, momentum7Vector);
	}

	public void printFinalMatrix(){
		System.out.println("Pnow\tPprev\tVprev\t\tmom22\tmom7");
		for(int i = 0; i < finalMatrix.getRowDimension(); i++){
			double pNow = finalMatrix.get(i, 0);
			double pPrev = finalMatrix.get(i, 1);
			double vPrev = finalMatrix.get(i, 2);
			double mom22 = finalMatrix.get(i, 3);
			double mom7 = finalMatrix.get(i, 4);
			System.out.printf("%.2f\t%.2f\t%9.0f\t%.4f\t%.4f\n", pNow, pPrev, vPrev, mom22, mom7);
		}
	}

	public Matrix concatLR(Matrix a, Matrix b){
		if(a.getRowDimension() != b.getRowDimension()){
			System.out.println("matrix rows don't match");
			return null;
		}
		int rows = a.getRowDimension();
		int columns = a.getColumnDimension() + b.getColumnDimension();
		Matrix merge = new Matrix(rows, columns);
		merge.setMatrix(0, rows-1, 0, a.getColumnDimension()-1, a);
		merge.setMatrix(0, rows-1, a.getColumnDimension(), columns-1, b);
		return merge;
	}



	public Matrix getClosingPrice(){
		int colIndex[] = {0};
		return ctmp.parseListToMatrix(parseList, colIndex);
	}

	public Matrix getPreviousPrice(){
		int colIndex[] = {11};
		return ctmp.parseListToMatrix(parseList, colIndex);
	}

	public Matrix getPreviousVolume(){
		int colIndex[] = {5};
		Matrix origV = ctmp.parseListToMatrix(parseList, colIndex);
		int numRows = origV.getRowDimension();
		Matrix prevV = new Matrix(numRows, 1);
		prevV.setMatrix(1, numRows-1, 0, 0, origV.getMatrix(0, numRows-2, 0, 0));
		return prevV;
	}

	public Matrix getMomentum(int delta){
		Matrix momTum = new Matrix(parseList.size(), 1);
		DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
		int flag;//will be 0 if central, 1 if forward, and -1 if backward

		for(int i = 0; i < momTum.getRowDimension(); i++){//giant loop through all i values of momTum matrix
			DateTime dNow = dtf.parseDateTime(parseList.get(i)[4]);
			flag = 0;

			int foreI = i, foreDelta = 0;
			for(int j = i+1; j < momTum.getRowDimension(); j++){//check forward days j until end
				DateTime dFore = dtf.parseDateTime(parseList.get(j)[4]);
				int dayDiff = Days.daysBetween(dNow, dFore).getDays();
				if(dayDiff >= delta){//when checking has reached far enough
					foreI = j;
					foreDelta = dayDiff;
					flag++;
					break;
				}
			}

			int backI = i, backDelta = 0;
			for(int j = i-1; j >= 0; j--){//check backward days j until start
				DateTime dBack = dtf.parseDateTime(parseList.get(j)[4]);
				int dayDiff = Days.daysBetween(dBack, dNow).getDays();
				if(dayDiff >= delta){//when checking has reached far enough
					backI = j;
					backDelta = dayDiff;
					flag--;
					break;
				}
			}

			double priceNow = 	closingPriceVector.get(i, 0);
			if(flag == 0){//central difference
				double priceFore = closingPriceVector.get(foreI, 0);
				double priceBack = closingPriceVector.get(backI, 0);
				double centralDiff = (priceFore - priceBack)/(foreDelta + backDelta);
				momTum.set(i, 0, centralDiff);
			}else if(flag > 0){//forward difference
				double priceFore = closingPriceVector.get(foreI, 0);
				double forwardDiff = (priceFore - priceNow)/(foreDelta);
				momTum.set(i, 0, forwardDiff);
			}else{//backward difference
				double priceBack = closingPriceVector.get(backI, 0);
				double backwardDiff = (priceNow - priceBack)/(backDelta);
				momTum.set(i, 0, backwardDiff);
			}
		}//end of huge for loop with index i

		return momTum;
	}

	void sandboxTesting(){
		//		How to use datetime formatter:
		DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
		DateTime dt1 = dtf.parseDateTime("05/25/2018");
		DateTime dt2 = dtf.parseDateTime("05/26/2017");
		System.out.println("Days between: " + Days.daysBetween(dt2, dt1).getDays());
	}
}
