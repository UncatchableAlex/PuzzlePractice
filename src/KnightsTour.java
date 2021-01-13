import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * In the mathematical sense, a knight's tour is a route that a knight can take around a chess board such that it touches
 * all squares on that board. In the context of this Java file, a KnightsTour is an object that is initialized with the
 * dimensions for a given sized chess board and a starting square. A KnightsTour's getTours() method can return all
 * knights' tours starting from the given square.
 *
 * @author Alex Meislich
 * @date 2021-01-11
 */
public class KnightsTour {
    /**board attribute*/
    protected final int height, width, startX, startY;
    /**algebraic notation of starting square*/
    protected final String algeNot;

    /**
     * This is the main method. By default it is set to find all knights tours on a 4x5 board.
     * @param args Nobody knows what these do...
     */
    public static void main(String[] args) {
        // declare the height and width of the board:
        final int width = 4, height = 5;
        // make a list that will hold all of the knights tours:
        List<List<String>> tours = new LinkedList<>();
        // make an ExecutorService with width*height threads. One for each starting square:
        ExecutorService elJefe = Executors.newFixedThreadPool(width * height);
        // make a list to hold the futures returned by the ExecutorService:
        List<Future<List<List<String>>>> futures = new LinkedList<>();

        // for each square on the board, make a new KnightsTour with that square set as its starting square:
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                KnightsTour kn = new KnightsTour(width, height, i, j);
                // submit that KnightsTour to run on a thread in the ExecutorService:
                Future<List<List<String>>> future = elJefe.submit(kn::getTours);
                // add the future result to the futures list:
                futures.add(future);
            }
        }

        // after all threads have begun working, loop through the futures and collect the results:
        for (Future<List<List<String>>> future : futures) {
            try {
                tours.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                elJefe.shutdown();
                System.out.println("Caught exception and shut down all processes. The exception was: " + e);
            }
        }

        // print everything:
        tours.forEach(System.out::println);
        System.out.println("\n Size: " + tours.size());

        // shutdown and exit (with code 0 ***fingers crossed***).
        elJefe.shutdown();
    }

    /**
     * Constructs a KnightsTour given the board size and the starting square in cartesian coordinates (the lower left
     * being a1).
     * @param width The width of the board.
     * @param height The height of the board.
     * @param startX The starting x coordinate of the tour.
     * @param startY The starting y coordinate of the tour.
     */
    public KnightsTour(int width, int height, int startX, int startY) {
        this.height = height;
        this.width = width;
        this.startX = startX;
        this.startY = startY;
        this.algeNot = (new Square(startX, startY, null)).algeNot;
        this.exceptionCheck();
    }

    /**
     * Constructs a KnightsTour given the board size and the starting square in algebraic notation ("a1", "b2", "d3" etc).
     * @param width The width of the board.
     * @param height The height of the board.
     * @param algeNot The algebraic notation of the start of the tour.
     */
    public KnightsTour(int width, int height, String algeNot) {
        Square tempSquare = new Square(algeNot, null);
        this.height = height;
        this.width = width;
        this.startX = tempSquare.x;
        this.startY = tempSquare.y;
        this.algeNot = algeNot;
        this.exceptionCheck();
    }

    /**
     * Check for exceptions privately and discretely.
     */
    private void exceptionCheck() {
        if (this.height <= 0 || this.width <= 0)
            throw new InvalidParameterException("Height and width MUST be greater than 0. Arguments `" + height +
                    ", " + width + "` are invalid");

        if (this.startX < 0 || this.startX >= this.width || this.startY < 0 || this.startY >= this.height)
            throw new InvalidParameterException("Start location MUST be greater than or equal to 0 and within " +
                    "height/width bounds. Arguments `" + this.height + ", " + this.width + "` are invalid");
    }

    /**
     * Performs a backtracking depth-first search to find all knights tours starting from the given square.
     * @return a list of String lists. Each String list is a sequence of moves that a knight can make to do its tour.
     */
    public List<List<String>> getTours() {
        Square startSquare = Objects.isNull(this.algeNot) ? new Square(this.startX, this.startY, null) :
                new Square(this.algeNot, null);

        Set<Square> startSet = Collections.singleton(startSquare);
        return this.getToursHelper(startSquare, new HashSet<>(startSet))
                .orElse(new LinkedList<>());
    }

    /**
     * Does all the recursion for getTours().
     * @param curr The Square currently being evaluated.
     * @param visited A Set containing all of the squares that have already been visited on the given knight's tour.
     * @return an Optional List of String Lists. Returns empty if no tours were found. Otherwise, returns all tours
     * listed in algebraic notation, format.
     */
    private Optional<List<List<String>>> getToursHelper(Square curr, Set<Square> visited) {
        // base case #1. If all Squares have been visited, return this tour in the appropriate format.
       if (visited.size() == this.width * this.height) {
           String res = curr.getTour();
           visited.remove(curr);
           List<String> ls = Collections.singletonList(res);
           List<List<String>> lsls = Collections.singletonList(ls);
           return Optional.of(lsls);
       }
       // get a list of all of the current Square's neighbors:
       List<Square> neighbors = curr.getNeighbors();

       // if the current Square doesn't have any neighbors, return empty (base case #2):
       if (neighbors.isEmpty()){
           visited.remove(curr);
           return Optional.empty();
       }

       // get a list of all successful tours stemming from this Square:
       List<List<String>> successfulPaths = neighbors
               // streamify neighbors to start our pipeline:
               .stream()
               // we only care about the neighbors that haven't been visited yet. Filter all others:
               .filter((Square neighbor) -> !visited.contains(neighbor))
               // add all previously unvisited neighbors to our visited map (as we will be visiting them shortly):
               .peek(visited::add)
               // recurse to visit the previously unvisited neighbors:
               .map(neighbor -> this.getToursHelper(neighbor, visited))
               // filter out the tours that return empty:
               .flatMap(Optional::stream)
               // unlist all nonempty tours so they can be collected into one large list of successful paths:
               .flatMap(Collection::stream)
               .collect(Collectors.toList());

       // if there were no successful paths, return empty, otherwise, return the successful paths:
       if (successfulPaths.isEmpty()) {
           visited.remove(curr);
           return Optional.empty();
       }
       visited.remove(curr);
       return Optional.of(successfulPaths);
    }

    /**
     * The Square class represents a square on a chessboard occupied by a knight. It can be constructed with either
     * cartesian or algebraic notation, and, once constructed, can return a list of all its neighbor Squares that can be
     * reached with a knight (through the use of getNeighbors()). Additionally, in order to construct a Square, it must
     * be supplied with a predecessor Square that the knight occupied one move ago. Through this mechanism, a knight's
     * path can be backtraced with repeated recursive calls to each Square's "prev" field.
     */
    private class Square {
        private final int x, y;
        private final String algeNot;
        private Square prev;

        /**
         * Constructs a Square with it's algebraic notation and a previous Square.
         * @param algeNot Algebraic notation coordinates of this Square.
         * @param prev The last Square that the knight occupied.
         */
        private Square(String algeNot, Square prev) {
            if (algeNot.length() != 2) {
                throw new RuntimeException("Algebraic notation invalid for argument: " + algeNot);
            }
            this.x = algeNot.charAt(0) - 'a';
            this.y = Integer.parseInt(algeNot.substring(1, 2)) - 1;
            this.algeNot = algeNot;
            this.init(prev);
        }

        /**
         * Constructs a Square with it's Cartesian coordinates.
         * @param x The x coordinate of it's position.
         * @param y The y coordinate of it's position.
         * @param prev The last Square that the knight occupied.
         */
        private Square(int x, int y, Square prev) {
            this.x = x;
            this.y = y;
            this.algeNot =  Character.toString('a' + x) + (y + 1);
            this.init(prev);
        }

        /**
         * Check for Exceptions and set prev.
         * @param prev The last Square that the knight occupied.
         */
        private void init(Square prev) {
            if (this.x >= KnightsTour.this.width || this.x < 0 || this.y >= KnightsTour.this.height || this.y < 0) {
                throw new IndexOutOfBoundsException("Out of bounds for Square: " + this.toString());
            }
            this.prev = prev;
        }

        /**
         * @return All neighbor Squares that can be reached by the knight currently on this Square.
         */
        private List<Square> getNeighbors() {
            List<Square> neighbors = new LinkedList<>();
            for (int i = -2; i <= 2; i += 4) {
                for (int j = -1; j <= 1; j += 2) {
                    try {
                        neighbors.add(new Square(this.x + i, this.y + j, this));
                    } catch (IndexOutOfBoundsException e) {
                        // do nothing. Those coordinates were invalid
                    }
                    try {
                        neighbors.add(new Square(this.x + j, this.y + i, this));
                    } catch (IndexOutOfBoundsException e) {
                        // do nothing. Those coordinates were invalid
                    }
                }
            }
            return neighbors;
        }

        /**
         * @return A list of the coordinates (in algebraic notation) of the Squares that the knight (currently on this
         * Square) has been to.
         */
        private String getTour() {
            List<String> tour = new LinkedList<>();
            Square curr = this;
            while (curr != null) {
                int x = curr.algeNot.charAt(0) - 'a';
                int y = Integer.parseInt(curr.algeNot.substring(1, 2));
                if (x >= KnightsTour.this.width || x < 0 || y > KnightsTour.this.height || y < 0) {
                    throw new RuntimeException("SOMETHING DIDN'T WORK!! Found: " + curr.algeNot);
                }
                tour.add(0, curr.algeNot);
                curr = curr.prev;
            }
            long distinct = tour.stream().distinct().count();
            if(distinct != (long) KnightsTour.this.width * KnightsTour.this.height) {
                throw new Error("SOMETHING DIDN'T WORK!! Expected: " + KnightsTour.this.width * KnightsTour.this.height + " elements, " +
                        "but found " + distinct + " elements!");
            }
            String tourStr = tour.toString();
            return tourStr.substring(1, tourStr.length() - 1);
        }

        @Override
        public String toString() {
            return this.algeNot + " (" + this.x + "," + this.y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Square) {
                Square otherSquare = (Square) o;
                return this.algeNot.equals(otherSquare.algeNot);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.algeNot.hashCode();
        }
    }
}