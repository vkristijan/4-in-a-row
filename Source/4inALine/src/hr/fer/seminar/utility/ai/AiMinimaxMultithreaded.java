package hr.fer.seminar.utility.ai;

import java.util.ArrayList;
import java.util.List;

import hr.fer.seminar.shapes.CellState;
import hr.fer.seminar.utility.GameUtility;
import hr.fer.seminar.utility.heuristic.Heuristic;

/**
 * This implementation of the {@link Ai} uses a
 * <a href="https://en.wikipedia.org/wiki/Minimax">Minimax algorithm</a> in
 * order to calculate the best move possible. This implementation is designed to
 * work using multiple threads in orders to speed up the calculation. After
 * reaching a certain depth of the search tree, a {@link Heuristic} algorithm
 * will be called to calculate the best move.
 * 
 * @author Kristijan Vulinovic
 */
public class AiMinimaxMultithreaded extends AbstractAi {
	/**
	 * Creates a new instance of this {@link Ai} algorithm. The
	 * {@link Heuristic} used in this instance is defined in the argument. The
	 * maximal depth of the search tree is also defined with an argument.
	 * 
	 * @param heuristic
	 *            the heuristic that should be used for this instance.
	 * @param depth
	 *            the maximal depth of the search tree.
	 */
	public AiMinimaxMultithreaded(Heuristic heuristic, int depth) {
		super();
		this.heuristic = heuristic;
		this.depth = depth;
	}

	/**
	 * Calculates the maximum possible score for the current player. In order to
	 * calculate the best move, this method will recursively calculate all the
	 * possible moves and evaluate their scores, until it reaches a certain
	 * depth in the search tree when it will just call a {@link Heuristic}. <br>
	 * This method is used to calculate the moves for the currently playing
	 * player.
	 * 
	 * @param moves
	 *            a list of all the moves played so far.
	 * @param currentPlayer
	 *            he player who should make his move.
	 * @param depth
	 *            The maximum depth that is allowed for the search tree. Once
	 *            the depth is reached, a heuristic approximation will be
	 *            called, as defined by {@link #heuristic}.
	 * @return the maximum possible score that the current player can achieve in
	 *         the given situation.
	 */
	private int max(List<Integer> moves, CellState currentPlayer, int depth) {
		if (depth > this.depth) {
			return heuristic.evaluatePosition(moves, currentPlayer);
		}

		List<Integer> possible = possibleMoves(moves);
		if (possible.size() == 0) return 0;

		int score;
		int size = possible.size();

		int bestValue = -INF;
		for (int i = 0; i < size; ++i) {
			List<Integer> tmpMoves = moves;
			tmpMoves.add(possible.get(i));

			if (GameUtility.checkWinner(GameUtility.listToBoard(tmpMoves, width, height)) == currentPlayer) {

				score = WIN_SCORE;
			} else {
				score = min(tmpMoves, GameUtility.switchPlayer(currentPlayer), depth + 1);
			}

			if (score > bestValue) {
				bestValue = score;
			}
			tmpMoves.remove(tmpMoves.size() - 1);
		}

		return bestValue;
	}

	/**
	 * Calculates the minimum possible score for the current player. In order to
	 * calculate the best move, this method will recursively calculate all the
	 * possible moves and evaluate their scores, until it reaches a certain
	 * depth in the search tree when it will just call a {@link Heuristic}. <br>
	 * This method is used to calculate the moves for the opponent.
	 * 
	 * @param moves
	 *            a list of all the moves played so far.
	 * @param currentPlayer
	 *            he player who should make his move.
	 * @param depth
	 *            The maximum depth that is allowed for the search tree. Once
	 *            the depth is reached, a heuristic approximation will be
	 *            called, as defined by {@link #heuristic}.
	 * @return the minimum possible score that the current player can achieve in
	 *         the given situation.
	 */
	private int min(List<Integer> moves, CellState currentPlayer, int depth) {
		if (depth > this.depth) {
			return heuristic.evaluatePosition(moves, currentPlayer);
		}

		List<Integer> possible = possibleMoves(moves);
		if (possible.size() == 0) return 0;

		int score;
		int size = possible.size();

		int bestValue = INF;
		for (int i = 0; i < size; ++i) {
			List<Integer> tmpMoves = moves;
			tmpMoves.add(possible.get(i));

			if (GameUtility.checkWinner(GameUtility.listToBoard(tmpMoves, width, height)) == currentPlayer) {

				score = -WIN_SCORE;
			} else {
				score = max(tmpMoves, GameUtility.switchPlayer(currentPlayer), depth + 1);
			}

			tmpMoves.remove(tmpMoves.size() - 1);

			if (score < bestValue) {
				bestValue = score;
			}
		}

		return bestValue;
	}

	@Override
	public int nextMove(List<Integer> moves, CellState currentPlayer) {
		List<Integer> possible = possibleMoves(moves);
		if (possible.size() == 0) {
			throw new IllegalStateException("The game is already over!");
		}

		int[] score = new int[possible.size()];

		int bestValue = -INF;
		int bestMove = -1;
		Thread[] thread = new Thread[score.length];

		for (int i = 0; i < score.length; ++i) {
			int iteration = i;
			List<Integer> tmpMoves = new ArrayList<>(moves);
			tmpMoves.add(possible.get(i));

			thread[i] = new Thread() {
				@Override
				public void run() {

					if (GameUtility.checkWinner(GameUtility.listToBoard(tmpMoves, width, height)) == currentPlayer) {

						score[iteration] = WIN_SCORE;
					} else {
						score[iteration] = min(tmpMoves, GameUtility.switchPlayer(currentPlayer), 1);
					}
				}
			};
			thread[i].start();
		}

		for (int i = 0; i < score.length; ++i) {
			try {
				thread[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (score[i] > bestValue) {
				bestValue = score[i];
				bestMove = possible.get(i);
			}
		}
		return bestMove;
	}
}
