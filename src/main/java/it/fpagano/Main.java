package it.fpagano;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class Main {
	public static void main(String[] args) {
		
		Map<String, Node> m = Map.of(
				"x", new Sum(new Operand(1), new Symbol("y")),
				"y", new Abs(new Difference(new Operand(1), new Operand(3)))
				);
		
		var map = new ExpressionMap(m);

		var root = new Difference(new Operand(3),
				new Multiplication(new Sum(new Operand(5), new Symbol("x")),
						new Sum(new Symbol("x"), new Symbol("y"), new Operand(1))));

		Expression e = new Expression(root);
		Number result = e.calc(map);
		System.out.println(result);
	}
}

record Expression(Node root) implements Node {
	public Number calc(ExpressionMap map) {
		return switch (root) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Expression o -> o.calc(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + root);
		};
	}

	public Set<String> getReferencedSymbols() {
		return Set.of();
	}
}

record ExpressionMap(Map<String, Node> map) {
	public Expression getExpression(String symbolName) {
		return new Expression(map.get(symbolName));
	}
}

interface Node { }

@FunctionalInterface
interface Operator extends Node {
	Integer reduce(ExpressionMap map);

	default Number nAryReduce(Number seed, ExpressionMap map, BinaryOperator<Number> f, Node...nodes) {
		Number n = seed;
		for (Node node : nodes) {
			n = f.apply(n, switch (node) {
				case Operand o -> o.val();
				case Operator o -> o.reduce(map);
				case Symbol o -> o.substitute(map).calc(map);
				default -> throw new IllegalStateException("Unexpected value: " + node);
			});
		}
		return n;
	}

	default Number bAryReduce(Node left, Node right, ExpressionMap map, BinaryOperator<Number> f) {
		Number a = switch (left) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + left);
		};

		Number b = switch (right) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + right);
		};

		return f.apply(a, b);
	}

	default Number unaryReduce(Node a, ExpressionMap map, UnaryOperator<Number> f) {
		return f.apply(switch (a) {
			case Operand o -> o.val() ;
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + a);
		});
	}
}

record Sum(Node... nodes) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		return nAryReduce(0, map, Math::addExact, nodes);
	}
}

record Multiplication(Node... nodes) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		return nAryReduce(1, map, Math::multiplyExact);
	}

}

record Difference(Node left, Node right) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		return bAryReduce(left, right, map, Math::subtractExact);
	}
}

record Division(Node left, Node right) implements Operator {
	@Override
	public Integer reduce(ExpressionMap map) {
		return bAryReduce(left, right, map, Math::);
	}
}

record Abs(Node a) implements Operator {
	@Override
	public Integer reduce(ExpressionMap map) {
		return unaryReduce(a, map, Math::abs);
	}
}

record Operand(Integer val) implements Node { }

record Symbol(String symbol) implements Node {

	public Expression substitute(ExpressionMap maps) {
		return maps.getExpression(symbol);
	}
	
}
