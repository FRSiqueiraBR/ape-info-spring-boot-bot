package br.com.frsiqueira.apeinfospringbootbot.service;

public abstract class CustomTimerTask {
    private String taskName = ""; ///< Task name
    private int times = 1;


    public CustomTimerTask(String taskName, int times) {
        this.taskName = taskName;
        this.times = times;
    }

    public String getTaskName() {
        return this.taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public int getTimes() {
        return this.times;
    }
    public void setTimes(int times) {
        this.times = times;
    }

    public void reduceTimes() {
        if (this.times > 0) {
            this.times -= 1;
        }
    }

    public abstract void execute();
}
