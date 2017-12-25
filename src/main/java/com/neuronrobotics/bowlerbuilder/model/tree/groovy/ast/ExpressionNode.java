package com.neuronrobotics.bowlerbuilder.model.tree.groovy.ast;

public class ExpressionNode extends ASTNode {

  private ASTNode statementNode;

  public ExpressionNode(ASTNode statementNode) {
    super();
    this.statementNode = statementNode;
  }

  public ASTNode getStatementNode() {
    return statementNode;
  }

}
