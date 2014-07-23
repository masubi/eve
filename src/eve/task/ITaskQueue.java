package eve.task;

/**
 * Created with IntelliJ IDEA.
 * User: masuij
 * Date: 6/24/14
 * Time: 1:56 PM
 * Project: oaf
 */
public interface ITaskQueue {
    public void pushTask(Task task);
    public Task popTask();
    public String toString();
    public Task peek();
}
