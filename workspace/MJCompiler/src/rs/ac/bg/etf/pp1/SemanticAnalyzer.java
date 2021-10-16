package rs.ac.bg.etf.pp1;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.CompilerError.CompilerErrorType;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class SemanticAnalyzer extends VisitorAdaptor {

	int nVars;
	int formalParamCnt = 0;
	Struct boolType = new Struct(Struct.Bool);
	Struct currentType = null;
	Obj currentMethod = null;
	
	boolean errorDetected = false;
	boolean returnFound = false;
	boolean insideSwitch = false;
	boolean insideDoWhile = false;
	
	List<Struct> actPars = new ArrayList<>();
	
	Logger log = Logger.getLogger(getClass());
	List<CompilerError> errors = new ArrayList<>();
	
	public List<CompilerError> getSemanticErrors(){
    	return errors;
    }
	
	public SemanticAnalyzer() {
		Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		int line = (info == null) ? 0: info.getLine();
		CompilerError error = new CompilerError(line, message, CompilerErrorType.SEMANTIC_ERROR);
		errors.add(error);
		StringBuilder msg = new StringBuilder(message);
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}
	
	public void report_syntax_error(String message, SyntaxNode info) {
		int line = (info == null) ? 0: info.getLine();
		CompilerError error = new CompilerError(line, message, CompilerErrorType.SYNTAX_ERROR);
		errors.add(error);
		StringBuilder msg = new StringBuilder(message);
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public void visit(Program program) {
		
		Obj mainMethod = Tab.find("main");
		if(mainMethod == Tab.noObj)
			report_error("Ne postoji void main() globalna funkcija", program);
		else {
				if( mainMethod.getKind() != Obj.Meth)
					report_error("Main nije metoda", program);
				if(	mainMethod.getType() != Tab.noType)
					report_error("Main metoda ima pogresan povratni tip, treba void", program);
				if(mainMethod.getLevel() != 0)
					report_error("Main metoda mora biti globalna", program);
		}
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}

	public void visit(ProgName progName) {
		if(Tab.currentScope().findSymbol(progName.getName()) == null)
			progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		else {
			progName.obj = Tab.noObj;
			report_error("Symbol sa imenom " + progName.getName() + " je vec deklarisan u trenutnom opsegu", progName);
		}
		
		Tab.openScope();     	
	}

	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getName());
		if (typeNode == Tab.noObj) {
			report_error("Nije pronadjen tip " + type.getName() + " u tabeli simbola", type);
			type.struct = Tab.noType;
		} 
		else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} 
			else {
				report_error("Greska: Ime " + type.getName() + " ne predstavlja tip ", type);
				type.struct = Tab.noType;
			}
		} 
		currentType = type.struct;
	}

	public void visit(MethodDecl methodDecl) {
		if (!returnFound && currentMethod.getType() != Tab.noType) {
			report_error("Funkcija " + currentMethod.getName() + " nema return iskaz!", methodDecl);
		}
		
		currentMethod.setLevel(formalParamCnt);
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		returnFound = false;
		currentMethod = null;
		formalParamCnt = 0;
	}

	public void visit(ReturnType methodTypeName) {
		if(Tab.currentScope().findSymbol(methodTypeName.getName()) == null) {
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getName(), methodTypeName.getType().struct);
			methodTypeName.obj = currentMethod;
		} else {
			currentMethod = Tab.noObj;
			report_error("Symbol sa imenom " + methodTypeName.getName() + " je vec deklarisan u trenutnom opsegu", methodTypeName);
		}
		Tab.openScope();
	}

	public void visit(ReturnVoid methodTypeName) {
		if(Tab.currentScope().findSymbol(methodTypeName.getName()) == null) {
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getName(), Tab.noType);
			methodTypeName.obj = currentMethod;
		}
		else {
			report_error("Symbol sa imenom " + methodTypeName.getName() + " je vec deklarisan u trenutnom opsegu", methodTypeName);
			currentMethod = Tab.noObj;
		}
		
		Tab.openScope();  
	}
	
	public void visit(ConstValChar constValChar) {
		if(!currentType.equals(Tab.charType)) {
			report_error("Tip terminala charConst mora biti ekvivalentant tipu Type",constValChar);
		}
	}
	
	public void visit(ConstValNum constValNum) {
		if(!currentType.equals(Tab.intType)) {
			report_error("Tip terminala numConst mora biti ekvivalentant tipu Type",constValNum);
		}
	}
	
	public void visit(ConstValBool constValBool) {
		if(!currentType.equals(this.boolType)) {
			report_error("Tip terminala boolConst mora biti ekvivalentant tipu Type",constValBool);
		}
	}
	
	public void visit(ConstDeclName constDeclName) {
		if(Tab.currentScope().findSymbol(constDeclName.getName()) == null) {
			
			constDeclName.obj = Tab.insert(Obj.Con, constDeclName.getName(), currentType);
			if(constDeclName.getConstVal() instanceof ConstValNum) {
				ConstValNum cvn = (ConstValNum) constDeclName.getConstVal();
				constDeclName.obj.setAdr(cvn.getValue());
			} else if (constDeclName.getConstVal() instanceof ConstValChar) {
				ConstValChar cvc = (ConstValChar) constDeclName.getConstVal();
				constDeclName.obj.setAdr(cvc.getValue());
			}  else if (constDeclName.getConstVal() instanceof ConstValBool) {
				ConstValBool cvb = (ConstValBool) constDeclName.getConstVal();
				Boolean bool = cvb.getValue();
				int value = 0;
				if(bool)
					value = 1;
				constDeclName.obj.setAdr(value);
			}
		}
		else {
			report_error("Symbol sa imenom " + constDeclName.getName() + " je vec deklarisan u trenutnom opsegu", constDeclName);
		}
		
	}
	
	public void visit(VarDecName varDecName) {
		if(Tab.currentScope().findSymbol(varDecName.getName()) == null) {
			if(varDecName.getOptArr() instanceof Array) {
				varDecName.obj = Tab.insert(Obj.Var, varDecName.getName(), new Struct(Struct.Array, currentType));
			} else {
				varDecName.obj = Tab.insert(Obj.Var, varDecName.getName(), currentType);
			}
		} else {
			report_error("Symbol sa imenom " + varDecName.getName() + " je vec deklarisan u trenutnom opsegu", varDecName);
		}
	}
	
	public void visit(FormParsR formParsR) {
		Obj obj = formParsR.getVarDecName().obj;
		formalParamCnt++;
		if(obj != null)
			obj.setFpPos(obj.getAdr() + 1);
	}
	
	public void visit(NoFromParsR noFormParsR) {
		Obj obj = noFormParsR.getVarDecName().obj;
		formalParamCnt = 1;
		if(obj != null)
			obj.setFpPos(1);
	}
	
	public void visit(DesignatorStatement designatorStatement) {
		
		Obj desStateObj = designatorStatement.getDesignator().obj;
		DesignStateOpt desStateOpt = designatorStatement.getDesignStateOpt();
		
		if(desStateOpt instanceof DesignStateOptInc || desStateOpt instanceof DesignStateOptDec) {
			if(!desStateObj.getType().equals(Tab.intType)) {
				report_error("Designator mora biti tipa int za ++ i --", designatorStatement);
			}
			if(desStateObj.getKind() != Obj.Var && desStateObj.getKind() != Obj.Elem && desStateObj.getKind() != Obj.Fld) {
				report_error("Designator u designator statementu mora da oznacava promenljivu, element niza ili polje unutar objekta", designatorStatement);
			}
		} else if (desStateOpt instanceof DesignStateOptAssign) {
			if(desStateObj.getKind() != Obj.Var && desStateObj.getKind() != Obj.Elem && desStateObj.getKind() != Obj.Fld) {
				report_error("Designator u designator statementu mora da oznacava promenljivu, element niza ili polje unutar objekta", designatorStatement);
			}
			DesignStateOptAssign desStateOptAssign = (DesignStateOptAssign) desStateOpt;
			if(!desStateOptAssign.getExpr().struct.assignableTo(desStateObj.getType())) {
				report_error("Tip neterminala Expr mora biti kompatibilan pri dodeli sa tipom neterminala Designator", designatorStatement);
			}
		} else if (desStateOpt instanceof DesginStateOptAct) {
			
			if(desStateObj.getKind() != Obj.Meth) {
				report_error("Designator mora oznacavati globalnu funkciju glavnog programa", designatorStatement);
			}
			
			if(desStateObj.getLevel() != actPars.size()) {
				report_error("Broj formalnih i stvarnih argumenata metode nije isti! " + desStateObj.getLevel() + " i " + actPars.size() , designatorStatement);
			} else {
				Collection<Obj> formalPars = desStateObj.getLocalSymbols();
				for(Obj formPar: formalPars) {
					if(formPar.getAdr() < actPars.size()){
						if(!actPars.get(formPar.getAdr()).equals(formPar.getType())) {
							report_error("Greska tip svakog stvarnog argumetna mora biti kompatibilan pri dodeli sa odgovarajucim tipom formlanog argumenta", designatorStatement);
						}
					}
				}
			}
			
			actPars.clear();
			
		} 
		
	}
	
	public void visit(LBraceSwitch lBraceSwitch) {
		this.insideSwitch = true;
	}
	
	public void visit(RBraceSwitch rBraceSwitch) {
		this.insideSwitch = false;
	}
	
	public void visit(StartDoWhile startDoWhile) {
		this.insideDoWhile = true;
	}
	
	public void visit(EndDoWhile endDoWhile) {
		this.insideDoWhile = false;
	}

	public void visit(StatementRead statementRead) {
		if(statementRead.getDesignator().obj.getKind() != Obj.Var &&
				statementRead.getDesignator().obj.getKind() != Obj.Elem &&
				statementRead.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Designator u read statementu mora da oznacava promenljivu, element niza ili polje unutar objekta", statementRead);
		}
		if(!statementRead.getDesignator().obj.getType().equals(Tab.intType) &&
				!statementRead.getDesignator().obj.getType().equals(Tab.charType) &&
				!statementRead.getDesignator().obj.getType().equals(this.boolType)) {
			report_error("Designator u read statementu mora biti tipa int,char ili bool", statementRead);
		}
	}
	
	public void visit(StatementPrint statementPrint) {
		if(!statementPrint.getExpr().struct.equals(Tab.charType) &&
				!statementPrint.getExpr().struct.equals(Tab.intType) &&
				!statementPrint.getExpr().struct.equals(this.boolType)) {
			report_error("Expr u print statementu mora biti tipa itn, char ili bool", statementPrint);
		}
	}
	
	public void visit(StatementReturn statementReturn){ //(StatementReturn) RETURN OptExpr SEMI
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		OptExpr optExpr = statementReturn.getOptExpr();
		if(optExpr instanceof OptExp &&
				!((OptExp) optExpr).getExpr().struct.compatibleWith(currMethType)) {
			report_error("Greska na liniji " + statementReturn.getLine() + " : " + "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
		}
		if(optExpr instanceof NoOptExp && currMethType != Tab.noType) {
			report_error("Greska na liniji " + statementReturn.getLine() + " : " + "metoda ne vraca nikakvu vrednost", null);
		}		  	     	
	}
	
	
	public void visit(StatementSwitch statementSwitch) { // (StatementSwitch) SWITCH LPAREN Expr RPAREN LBraceSwitch ExprCaseRec RBraceSwitch
		if(statementSwitch.getExpr().struct != Tab.intType) {
			report_error("Expr mora biti celobrojnog tipa.", statementSwitch);
		}
	
	}
	
	public void visit(StatementCont statementCont) {
		if(!insideDoWhile) {
			report_error("Iskaz continue se moze koristiti samo unutar do-while petlje.", statementCont);
		}
	}
	
	public void visit(StatementBreak statementBreak) {
		if(!insideDoWhile && !insideSwitch) {
			report_error("Iskaz break se moze koristiti samo unutar do-while petlje i visestrukog grananja (switch).", statementBreak);
		}
	}
	
	public void visit(ActParsR actParsR) {
		actPars.add(actParsR.getExpr().struct);
	}
	
	public void visit(ActParsNoR actParsNoR) {
		actPars.add(actParsNoR.getExpr().struct);
	}

	public void visit(CondFact condFact) {
		Expr firstExpr = condFact.getExpr();
		condFact.struct = this.boolType;
		if(condFact.getOptRelop() instanceof OptRel) {
			OptRel optRel = (OptRel) condFact.getOptRelop();
			if(!optRel.getExpr().struct.compatibleWith(firstExpr.struct)) {
				report_error("Tipovi oba izraza u CondFact moraju biti kompatiblni", condFact);
				condFact.struct = Tab.noType;
			}
			else if(firstExpr.struct.getKind() == Struct.Array && !(optRel.getRelop() instanceof RelopEq || optRel.getRelop() instanceof RelopNotEq)) {
				report_error("Uz promenljive tipa niza mogu se koristiti samo != i ==", condFact);
				condFact.struct = Tab.noType;
			}
			
		} else {
			if(!firstExpr.struct.equals(this.boolType)) {
				report_error("Nije tip bool", condFact);
				condFact.struct = Tab.noType;
			}
		}
		
	}

	public void visit(ExprOptR addExpr) { // = (ExprOptR) Expr Addop Term
		Struct exprType = addExpr.getExpr().struct; 
		Struct termType = addExpr.getTerm().struct;
		if (exprType.equals(termType) && exprType == Tab.intType) 
			addExpr.struct = exprType;
		else {
			report_error("Nekompatibilni tipovi u izrazu uz addOp.", addExpr);
			addExpr.struct = Tab.noType;
		} 
	}

	public void visit(NoExprOptR noExprOptR) {  
		if(noExprOptR.getOptMinus() instanceof OptMin) { 
			Struct termType = noExprOptR.getTerm().struct;
			if(termType == Tab.intType) {
				noExprOptR.struct = termType;
			} else {
				report_error("Nekompatibilni tipovi u izrazu uz OptMin.", noExprOptR);
				noExprOptR.struct = Tab.noType;
			}
		} else { 
			noExprOptR.struct = noExprOptR.getTerm().struct;
		}
	}
	
	public void visit(TermOptR termOptR) { //  (TermOptR) Term Mulop Factor
		Struct termType = termOptR.getTerm().struct;
		Struct factorType = termOptR.getFactor().struct;
		if(termType.equals(factorType) && termType == Tab.intType) {
			termOptR.struct = termType;
		} else if (termType.equals(factorType) && termType.equals(new Struct(Struct.Array, Tab.intType))) { 
			termOptR.struct = Tab.intType; 
		} else if (factorType.equals(Tab.intType) && termType.equals(new Struct(Struct.Array, Tab.intType))) { 
			termOptR.struct = termType;
		} else {
			report_error("Nekompatibilni tipovi u izrazu uz mulOp.", termOptR);
			termOptR.struct = Tab.noType;
		}
		
	}
	
	public void visit(NoTermOptR noTermOptR) { // (NoTermOptR) Factor
		noTermOptR.struct = noTermOptR.getFactor().struct;    	
	}
	
	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}
	
	public void visit(FactorBool factorBool) {
		factorBool.struct = this.boolType;
	}
	
	public void visit(FactorChar factorChar) {
		factorChar.struct = Tab.charType;
	}
	
	public void visit(FactorNum factorNum) {
		factorNum.struct = Tab.intType;
	}
	
	public void visit(FactorNew factorNew) { // (FactorNew) NEW Type FactorExprOpt
		factorNew.struct = new Struct(Struct.Array, currentType);
		if(factorNew.getFactorExprOpt() instanceof FactorExprOp) {
			FactorExprOp feo = (FactorExprOp) factorNew.getFactorExprOpt();
			if(!feo.getExpr().struct.equals(Tab.intType))
				report_error("Kad pravimo niz sa new moramo dati int za broj elemenata niza", factorNew);
		}
	}
	
	
	public void visit(FactorDesignator factorDesginator) { // (FactorDesignator) Designator FactorActParsOpt
		factorDesginator.struct = factorDesginator.getDesignator().obj.getType();
		if(factorDesginator.getFactorActParsOpt() instanceof FactorActPars) { // Function call
			
			if(factorDesginator.getDesignator().obj.getKind() != Obj.Meth) { 
				report_error("Designator mora oznacavati globalnu funkciju glavnog programa", factorDesginator);
			}
			
			if(factorDesginator.getDesignator().obj.getLevel() != actPars.size()) {
				report_error("Broj formalnih i stvarnih argumenata metode nije isti! " + factorDesginator.getDesignator().obj.getLevel() + " i " + actPars.size() , factorDesginator);
			} else {
				Collection<Obj> formalPars = factorDesginator.getDesignator().obj.getLocalSymbols();
				for(Obj formPar: formalPars) {
					if(formPar.getAdr() < actPars.size()){
						if(!actPars.get(formPar.getAdr()).equals(formPar.getType())) { 
							report_error("Greska tip svakog stvarnog argumetna mora biti kompatibilan pri dodeli sa odgovarajucim tipom formlanog argumenta", factorDesginator);
						}
					}
				}
			}
			
			actPars.clear();
			
		}
		
	}

	public void visit(NoDesignatorOptR designator){
		Obj obj = Tab.find(designator.getName());
		if (obj == Tab.noObj) { 
			report_error("Simbol sa imenom "+designator.getName()+" nije deklarisan! ", designator);
		} else {
			DumpSymbolTableVisitor dstv = new DumpSymbolTableVisitor();
			dstv.visitObjNode(obj);
			switch(obj.getKind()) {
			case Obj.Con:
				report_info("Detekovano koriscenje konstantne " + dstv.getOutput(), designator);
				break;
			case Obj.Var:
				if(obj.getLevel() == 0) {
					report_info("Detekovano koriscenje globalne promenljive " + dstv.getOutput(), designator);
				} else if (obj.getFpPos() == 0) {
					report_info("Detekovano koriscenje lokalne promenljive " + dstv.getOutput(), designator);
				} else {
					report_info("Detekovano koriscenje parametra metode " + dstv.getOutput(), designator);
				}
				break;
			case Obj.Meth:
				report_info("Detekovano pozivanje metode " + dstv.getOutput(), designator);
				break;
			}
		}
		designator.obj = obj;
	}
	
	public void visit(DesignatorOptR designatorOptR){
		
		if(designatorOptR.getDesignatorOpt() instanceof DesignatorOptExpr) {
			Obj obj = designatorOptR.getDesignatorArr().getDesignator().obj;
			if(obj.getType().getKind() != Struct.Array) {
				report_error("Greska na liniji " + designatorOptR.getLine() + " : " + "tip neterminala designator mora biti niz", designatorOptR);
				designatorOptR.obj = Tab.noObj;
			} else {
				designatorOptR.obj = new Obj(Obj.Elem, "", obj.getType().getElemType());
				DumpSymbolTableVisitor dstv = new DumpSymbolTableVisitor();
				dstv.visitObjNode(obj);
				report_info("Detekovan pristup elementu niza " + dstv.getOutput(), designatorOptR);
			}
		}
	}
	
	public void visit(DesignatorOptExpr designatorOptExpr) {
		if(!designatorOptExpr.getExpr().struct.equals(Tab.intType)) {
			report_error("Greska na liniji " + designatorOptExpr.getLine() + " : " + "tip neterminala expr mora biti int", designatorOptExpr);
		} 
	}
	
	public boolean passed() {
		return !errorDetected;
	}
	
	// Visits for syntax errors
	
	public void visit(VarDeclError varDeclError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom definicije globalne promenljive do ;",varDeclError);
		report_syntax_error("Sintaksna greska u definiciji globalne promenljive", varDeclError);
	}
	
	public void visit(VarDecMoreError varDecMoreError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom definicje globalne promenljive do ,",varDecMoreError);
		report_syntax_error("Sintaksna greska prilikom definicije globalne promenljive", varDecMoreError);
	}
	
	public void visit(MethodDeclError methodDeclError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom deklaracije formalnih parametara do )",methodDeclError);
		report_syntax_error("Sintaksna greska prilikom deklaracije formalnih parametara", methodDeclError);
	}
	
	public void visit(FromParsRError fromParsRError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom deklaracije formalnih parameteara do ,",fromParsRError);
		report_syntax_error("Sintaksna greska prilikom deklaracije formalnih parameteara", fromParsRError);
	}
	
	public void visit(IfCondError ifCondError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom provere logickog izraza unutar if konstrukcije, oporavak do ;",ifCondError);
		report_syntax_error("Sintaksna greska prilikom provere logickog izraza unutar if konstrukcije", ifCondError);
	}
	
	public void visit(DesignStateOptAssignError designStateOptAssignError) {
		report_info("Uspesan oporavak od sintaksne greske prilikom konstrukcije iskaza dodele, oporavak do ,",designStateOptAssignError);
		report_syntax_error("Sintaksna greska prilikom konstrukcije iskaza dodele", designStateOptAssignError);
	}
}

