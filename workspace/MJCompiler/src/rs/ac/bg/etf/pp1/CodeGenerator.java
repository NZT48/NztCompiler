package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPc;
	private int op;
	private Stack<List<Integer>> adrAnd = new Stack<>();
	private Stack<Integer> adrElse = new Stack<>();
	private List<Integer> adrOr = new ArrayList<>();
	private Stack<Integer> adrDoWhile = new Stack<>();
	private boolean insideWhileCondition = false;
	private Stack<List<Integer>> adrBreak = new Stack<>();
	private Stack<List<Integer>> adrContinue = new Stack<>();
	
	public CodeGenerator() {
		
		Tab.chrObj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.put(Code.load_n);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Tab.ordObj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.put(Code.load_n);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Tab.lenObj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public int getMainPc() {
		return mainPc;
	}
	
	@Override
	public void visit(ReturnVoid methodTypeName) {
		methodTypeName.obj.setAdr(Code.pc);
		if ("main".equalsIgnoreCase(methodTypeName.getName())) {
			mainPc = Code.pc;
		}
		
		int formalParamCnt = 0;
		for(Obj o:methodTypeName.obj.getLocalSymbols() ) {
			if(o.getFpPos() > 0) {
				formalParamCnt++;
			}
		}
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(formalParamCnt);
		Code.put(methodTypeName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(ReturnType methodTypeName) {
		methodTypeName.obj.setAdr(Code.pc);
		
		int formalParamCnt = 0;
		for(Obj o:methodTypeName.obj.getLocalSymbols() ) {
			if(o.getFpPos() > 0) {
				formalParamCnt++;
			}
		}
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(formalParamCnt);
		Code.put(methodTypeName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodDecl MethodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(StatementReturn ReturnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(DesignatorStatement designatorStatement) {
		Obj obj = designatorStatement.getDesignator().obj;
		if(designatorStatement.getDesignStateOpt() instanceof DesignStateOptAssign)
			Code.store(obj);
		else if(designatorStatement.getDesignStateOpt() instanceof DesginStateOptAct) {
			int offset = obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset);
			if(!obj.getType().equals(Tab.noObj)) { 
				Code.put(Code.pop);
			}
		} else if(designatorStatement.getDesignStateOpt() instanceof DesignStateOptInc) {
			if(obj.getKind() == Obj.Elem) {
				Code.put(Code.dup2);
			}
			Code.load(obj);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.store(obj);
		}
		else {
			if(obj.getKind() == Obj.Elem) {
				Code.put(Code.dup2);
			}
			Code.load(obj);
			Code.loadConst(1);
			Code.put(Code.sub);
			Code.store(obj);
		}
	}
	
	@Override
	public void visit(FactorNum factorNum) {
		Code.loadConst(factorNum.getN1()); 
	}
	
	@Override
	public void visit(FactorChar factorNum) {
		Code.loadConst(factorNum.getC1()); 
	}
	
	@Override
	public void visit(FactorBool factorNum) {
		Code.loadConst(factorNum.getB1() ? 1 : 0); 
	}
	
	@Override
	public void visit(FactorDesignator factorDesignator) {
		Obj obj = factorDesignator.getDesignator().obj;
		if(factorDesignator.getFactorActParsOpt() instanceof FactorActPars) {
			int offset = obj.getAdr() - Code.pc; 
			Code.put(Code.call);
			Code.put2(offset);
		} else
			Code.load(obj);
	}
	
	@Override
	public void visit(FactorNew factorNew) {
		if(factorNew.getFactorExprOpt() instanceof FactorExprOp) {
			Code.put(Code.newarray);
			if(factorNew.getType().struct.equals(Tab.charType))
				Code.put(0);
			else
				Code.put(1);
		}
	}
	
	@Override
	public void visit(StatementPrint printStmt) {
		if(printStmt.getNumConstOpt() instanceof NumConst) {
			NumConst nc = (NumConst) printStmt.getNumConstOpt();
			Code.loadConst(nc.getN1());
		} else 
			Code.loadConst(1);
		if(printStmt.getExpr().struct.equals(Tab.charType))
			Code.put(Code.bprint);
		else
			Code.put(Code.print);
	}
	
	@Override
	public void visit(StatementRead readStmt) {
		Obj obj = readStmt.getDesignator().obj;
		if(obj.getType().equals(Tab.charType))
			Code.put(Code.bread);
		else
			Code.put(Code.read);
		Code.store(obj);
		
	}
	
	@Override
	public void visit(DesignatorArr designatorArr) {
		Code.load(designatorArr.getDesignator().obj);
	}
	
	@Override
	public void visit(ExprOptR addExpr) {
		if(addExpr.getAddop() instanceof AddopPlus)
			Code.put(Code.add);
		else
			Code.put(Code.sub);
	}
	
	@Override
	public void visit(TermOptR mulOp) {
		if(mulOp.getMulop() instanceof MulopMul) {
			Struct termType = mulOp.getTerm().struct;
			Struct factorType = mulOp.getFactor().struct;
			if(termType.equals(factorType) && termType == Tab.intType)
				Code.put(Code.mul);
			else if (termType.equals(factorType) && termType.equals(new Struct(Struct.Array, Tab.intType))) { 
				Code.put(Code.enter);
				Code.put(2);
				Code.put(3);
				
				/*
				 * p0 - adr of vector1
				 * p1 - adr of vector2
				 * p2 - counter
				 */
				
				// counter = len(vector1) - 1
				Code.put(Code.load_n);
				Code.put(Code.arraylength);
				Code.loadConst(1);
				Code.put(Code.sub);
				Code.put(Code.store_2);
				
				// ret value on stack
				Code.loadConst(0);
				
				// while (counter > 0)
				int whileCond = Code.pc;
				Code.put(Code.load_2);
				Code.loadConst(0);
				Code.putFalseJump(Code.ge, 0);
				int whileStart = Code.pc - 2;
				
				// vector1[counter]
				Code.put(Code.load_n);
				Code.put(Code.load_2);
				Code.put(Code.aload);
				
				// vector2[counter]
				Code.put(Code.load_1);
				Code.put(Code.load_2);
				Code.put(Code.aload);
				
				// ret_val += vector1[counter] + vector2[counter]
				Code.put(Code.mul);
				Code.put(Code.add);
				
				// Counter = counter - 1
				Code.put(Code.load_2);
				Code.loadConst(1);
				Code.put(Code.sub);
				Code.put(Code.store_2);
				
				// End of while
				Code.putJump(whileCond);
				Code.fixup(whileStart);
				
				// Ret value will be on stack
				
				Code.put(Code.exit);
			 
			} else if (factorType.equals(Tab.intType) && termType.equals(new Struct(Struct.Array, Tab.intType))) { 
				Code.put(Code.enter);
				Code.put(2);
				Code.put(4);
				
				/*
				 * p0 - adr of vector
				 * p1 - scalar
				 * p2 - adr of return array
				 * p3 - counter
				 */
				
				// Creating new array
				Code.put(Code.load_n);
				Code.put(Code.arraylength);
				Code.put(Code.newarray);
				Code.put(1);
				Code.put(Code.store_2);
				
				// set counter at len of array - 1
				Code.put(Code.load_n);
				Code.put(Code.arraylength);
				Code.loadConst(1);
				Code.put(Code.sub);
				Code.put(Code.store_3);
				
				// While(Counter > 0)
				int whileCond = Code.pc;
				Code.put(Code.load_3);
				Code.loadConst(0);
				Code.putFalseJump(Code.ge, 0);
				int whileStart = Code.pc - 2;
				
				// prepare args for ret array
				Code.put(Code.load_2);
				Code.put(Code.load_3);
				
				// get vector[counter]
				Code.put(Code.load_n);
				Code.put(Code.load_3);
				Code.put(Code.aload);
				
				// vector[counter] * scalar
				Code.put(Code.load_1);
				Code.put(Code.mul);
				
				// ret_arr[counter] = vector[counter] * scalar
				Code.put(Code.astore);
				
				// counter = counter - 1
				Code.put(Code.load_3);
				Code.loadConst(1);
				Code.put(Code.sub);
				Code.put(Code.store_3);
				
				// End of while
				Code.putJump(whileCond);
				Code.fixup(whileStart);
				
				// Return adr of new array
				Code.put(Code.load_2);
				
				Code.put(Code.exit);
			}
		}
		else if(mulOp.getMulop() instanceof MulopDiv)
			Code.put(Code.div);
		else
			Code.put(Code.rem);
	}

	@Override
	public void visit(NoExprOptR noExprOptR) {
		if(noExprOptR.getOptMinus() instanceof OptMin)
			Code.put(Code.neg);
	}
	
	@Override
	public void visit(IfCond ifCondition) {
		Code.putFalseJump(op, 0);
		adrAnd.peek().add(Code.pc - 2);
		for(int adr: adrOr) {
			Code.fixup(adr);
		}
		adrOr.clear();
	}
	
	@Override
	public void visit(StatementIf statementIf) {
		List<Integer> adrList = adrAnd.pop();
		for(int adr: adrList) {
			Code.fixup(adr);
		}
	}
	
	@Override
	public void visit(Else else_st) {
		Code.putJump(0);
		adrElse.push(Code.pc - 2);
		List<Integer> adrList = adrAnd.pop();
		for(int adr: adrList) {
			Code.fixup(adr);
		}
	}
	
	@Override
	public void visit(StatementIfElse statementIfElse) {
		Code.fixup(adrElse.pop());
	}
	
	@Override
	public void visit(OptRel optRel) {
		Relop relop = optRel.getRelop();
		if(relop instanceof RelopEq) {
			op = Code.eq;
		} else if (relop instanceof RelopGreater) {
			op = Code.gt;
		} else if (relop instanceof RelopNotEq) {
			op = Code.ne;
		} else if (relop instanceof RelopGreaterEq) {
			op = Code.ge;
		} else if (relop instanceof RelopLess) {
			op = Code.lt;
		} else if (relop instanceof RelopLessEq) {
			op = Code.le;
		}
	}
	
	@Override
	public void visit(NoOptRel noOptRel) {
		op = Code.eq;
		Code.loadConst(1);
	}
	
	@Override
	public void visit(And and) {
		Code.putFalseJump(op, 0);
		
		adrAnd.peek().add(Code.pc - 2);
	}
	
	@Override
	public void visit(CondTermNoMore condTermNoMore) {
		adrAnd.push(new ArrayList<>());
	}
	
	@Override
	public void visit(Or or) {
		if(!insideWhileCondition) {
			putTrueJump(op, 0);
			adrOr.add(Code.pc-2);
		} else {
			putTrueJump(op, adrDoWhile.peek());
		}
		List<Integer> adrList = adrAnd.pop();
		for(int adr: adrList) {
			Code.fixup(adr);
		}
	}
	
	public static void putTrueJump (int op, int adr) {
		Code.put(Code.jcc + op); Code.put2(adr-Code.pc+1);
	}
	
	@Override
	public void visit(StartDoWhile startDoWhile) {
		adrDoWhile.push(Code.pc);
		adrBreak.push(new ArrayList<>());
		adrContinue.push(new ArrayList<>());
	}
	
	
	@Override
	public void visit(WhileCondition whileCondition) {
		putTrueJump(op, adrDoWhile.pop());
		List<Integer> adrList = adrAnd.pop();
		for(int adr: adrList) {
			Code.fixup(adr);
		}
		List<Integer> adrBreakList = adrBreak.pop();
		for(int adr: adrBreakList) {
			Code.fixup(adr);
		}
		insideWhileCondition = false;
		
	}
	
	@Override
	public void visit(EndDoWhile endDoWhile) {
		insideWhileCondition = true;
		List<Integer> adrContList = adrContinue.pop();
		for(int adr: adrContList) {
			Code.fixup(adr);
		}
	}
	
	@Override
	public void visit(StatementBreak statementBreak) {
		Code.putJump(0);
		adrBreak.peek().add(Code.pc-2);
	}
	
	@Override
	public void visit(StatementCont statementCont) {
		Code.putJump(0);
		adrContinue.peek().add(Code.pc-2);
	}

	
	
	
}
