package it.fpagano;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class Main {
	public static void main(String[] args) {
		
		Map<String, Expression> m = Map.of(
				"x", new Expression(new Sum(new Operand(1d), new Symbol("y"))), // x = 3
				"y", new Expression(new Abs(new Difference(new Operand(1d), new Operand(3d)))) // y = 2
				);
		
		var expressionMap = new ExpressionMap(m);

		var root = new Difference(new Operand(3d),
				new Multiplication(new Sum(new Operand(5d), new Symbol("x")),
						new Sum(new Symbol("x"), new Symbol("y"), new Operand(1d))));

		var expression = new Expression(root);
		System.out.println("Referenced Symbols are = " + expression.getReferencedSymbols());
		var result = expression.calc(expressionMap);
		System.out.println("expression result is = " + result);
	}
}

record Expression(Node root) implements Node {
	public Double calc(ExpressionMap map) {
		return switch (root) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Expression o -> o.calc(map);
			case Symbol o -> o.substitute(map).calc(map);
		};
	}

	public Set<String> getReferencedSymbols() {
		return getReferencedSymbolsRecursive(root);
	}

	public Set<String> getReferencedSymbolsRecursive(Node node) {
		Set<String> seed = new HashSet<>();
		return switch (node) {
			case BnaryOperator o -> mergeSet(getReferencedSymbolsRecursive(o.getLeft()), getReferencedSymbolsRecursive(o.getRight()));
			case UOperator o -> getReferencedSymbolsRecursive(o.getNode());
			case NaryOperator o -> Arrays.stream(o.getNodes())
					.reduce(seed,
						(s, n) -> mergeSet(s, getReferencedSymbolsRecursive(n)),
							Expression::mergeSet);
			case Expression o -> o.getReferencedSymbols();
			case Symbol o -> Set.of(o.symbol());
			case Operand o -> Set.of();
		};
	}

	private static Set<String> mergeSet(Set<String> a, Set<String> b) {
		return new HashSet<>() {
			{
				addAll(a);
				addAll(b);
			}
		};
	}
}

record ExpressionMap(Map<String, Expression> map) {
	public Expression getExpression(String symbolName) {
		return map.get(symbolName);
	}
}

sealed interface Node { }

sealed interface Operator extends Node {
	Double reduce(ExpressionMap map);

	default Double reduce (Node node, ExpressionMap map) {
			return switch (node) {
				case Operand o -> o.val();
				case Operator o -> o.reduce(map);
				case Symbol o -> o.substitute(map).calc(map);
				case Expression o -> o.calc(map);
			};
	}
}

sealed interface BnaryOperator extends Operator {

	Node getLeft();
	Node getRight();

	default Double bAryReduce(Node left, Node right, ExpressionMap map, BinaryOperator<Double> f) {
		Double a = reduce(left, map);
		Double b = reduce(right, map);
		return f.apply(a, b);
	}
}

sealed interface UOperator extends Operator {

	Node getNode();
	default Double unaryReduce(Node a, ExpressionMap map, UnaryOperator<Double> f) {
		return f.apply(reduce(a, map));
	}
}
sealed interface NaryOperator extends Operator {

	Node[] getNodes();
	default Double nAryReduce(Double seed, ExpressionMap map, BinaryOperator<Double> f, Node...nodes) {
		Double n = seed;

		for (Node node : nodes) {
			n = f.apply(n, reduce(node, map));
		}
		return n;
	}
}

record Sum(Node... nodes) implements NaryOperator {

	@Override
	public Double reduce(ExpressionMap map) {
		return nAryReduce(0d, map, Double::sum, nodes);
	}


	@Override
	public Node[] getNodes() {
		return nodes;
	}
}

record Multiplication(Node... nodes) implements NaryOperator {


	@Override
	public Double reduce(ExpressionMap map) {
		return nAryReduce(1d, map, (a, b) -> a * b, nodes);
	}

	@Override
	public Node[] getNodes() {
		return nodes;
	}
}

record Difference(Node left, Node right) implements BnaryOperator {

	@Override
	public Double reduce(ExpressionMap map) {
		return bAryReduce(left, right, map, (a, b) -> a - b);
	}

	@Override
	public Node getLeft() {
		return left;
	}

	@Override
	public Node getRight() {
		return right;
	}
}

record Division(Node left, Node right) implements BnaryOperator {
	@Override
	public Double reduce(ExpressionMap map) {
		return bAryReduce(left, right, map, (a, b) -> a / b);
	}

	@Override
	public Node getLeft() {
		return left;
	}

	@Override
	public Node getRight() {
		return right;
	}
}

record Abs(Node a) implements UOperator {
	@Override
	public Double reduce(ExpressionMap map) {
		return unaryReduce(a, map, Math::abs);
	}

	@Override
	public Node getNode() {
		return a;
	}
}

record Operand(Double val) implements Node { }

record Symbol(String symbol) implements Node {

	public Expression substitute(ExpressionMap maps) {
		return maps.getExpression(symbol);
	}
	
}
