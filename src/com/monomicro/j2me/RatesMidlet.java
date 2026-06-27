package com.monomicro.j2me;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Downloads and displays currency rates from a simple text feed.
 */
public final class RatesMidlet extends MIDlet implements CommandListener, Runnable {
    private static final String URL_PROPERTY = "Rates-URL";
    private static final String DEFAULT_URL = "http://monomicro.pluxa.cc/rates.txt";
    private static final String TITLE_RATES = "Currency rates";
    private static final String TITLE_LOADING = "Loading...";
    private static final String TITLE_ERROR = "Error";
    private static final String TEXT_DOWNLOADING = "Downloading rates...";
    private static final String TEXT_BUY = "Buy";
    private static final String TEXT_SELL = "Sell";
    private static final String COMMAND_REFRESH = "Refresh";
    private static final String COMMAND_EXIT = "Exit";
    private static final String ERROR_NETWORK = "Network error. Check your connection and try again.";
    private static final String ERROR_HTTP = "Server error. Please try again later.";
    private static final String ERROR_INVALID_RESPONSE = "Invalid server response.";
    private static final String ERROR_EMPTY_RESPONSE = "No rates available.";
    private static final String ERROR_UNKNOWN = "Unable to load rates.";

    private final Command refreshCommand = new Command(COMMAND_REFRESH, Command.SCREEN, 1);
    private final Command exitCommand = new Command(COMMAND_EXIT, Command.EXIT, 2);
    private final Command retryCommand = new Command(COMMAND_REFRESH, Command.SCREEN, 1);

    private Display display;
    private List ratesList;
    private Form loadingForm;
    private Form errorForm;
    private String ratesUrl;
    private boolean loading;

    /**
     * Starts the MIDlet and loads rates on the first launch.
     *
     * @throws MIDletStateChangeException if the MIDlet cannot be started
     */
    protected void startApp() throws MIDletStateChangeException {
        display = Display.getDisplay(this);
        ratesUrl = getAppProperty(URL_PROPERTY);
        if (ratesUrl == null || ratesUrl.length() == 0) {
            ratesUrl = DEFAULT_URL;
        }

        if (ratesList == null) {
            createScreens();
            startLoadingRates();
        } else {
            display.setCurrent(ratesList);
        }
    }

    /**
     * Called by the application manager when the MIDlet is paused.
     */
    protected void pauseApp() {
    }

    /**
     * Called by the application manager when the MIDlet is destroyed.
     *
     * @param unconditional true when the MIDlet must release resources
     * @throws MIDletStateChangeException if the MIDlet refuses destruction
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    /**
     * Handles Refresh and Exit commands from all screens.
     *
     * @param command selected command
     * @param displayable current displayable
     */
    public void commandAction(Command command, Displayable displayable) {
        if (command == exitCommand) {
            notifyDestroyed();
        } else if (command == refreshCommand || command == retryCommand) {
            startLoadingRates();
        }
    }

    /**
     * Downloads and renders rates on a background thread.
     */
    public void run() {
        try {
            Vector rates = downloadRates();
            showRates(rates);
        } catch (LoadException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError(ERROR_UNKNOWN);
        } finally {
            loading = false;
        }
    }

    private void createScreens() {
        ratesList = new List(TITLE_RATES, List.IMPLICIT);
        ratesList.addCommand(refreshCommand);
        ratesList.addCommand(exitCommand);
        ratesList.setCommandListener(this);

        loadingForm = new Form(TITLE_LOADING);
        loadingForm.append(new Gauge(TEXT_DOWNLOADING, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
        loadingForm.addCommand(exitCommand);
        loadingForm.setCommandListener(this);

        errorForm = new Form(TITLE_ERROR);
        errorForm.addCommand(retryCommand);
        errorForm.addCommand(exitCommand);
        errorForm.setCommandListener(this);
    }

    private void startLoadingRates() {
        if (loading) {
            return;
        }

        loading = true;
        display.setCurrent(loadingForm);
        Thread thread = new Thread(this);
        thread.start();
    }

    private Vector downloadRates() throws LoadException {
        HttpConnection connection = null;
        InputStream input = null;

        try {
            connection = (HttpConnection) Connector.open(ratesUrl, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);

            int code = connection.getResponseCode();
            if (code != HttpConnection.HTTP_OK) {
                throw new LoadException(ERROR_HTTP);
            }

            input = connection.openInputStream();
            return parseResponse(readText(input));
        } catch (LoadException e) {
            throw e;
        } catch (IOException e) {
            throw new LoadException(ERROR_NETWORK);
        } finally {
            closeInput(input);
            closeConnection(connection);
        }
    }

    private String readText(InputStream input) throws IOException {
        StringBuffer buffer = new StringBuffer();
        int value;

        while ((value = input.read()) != -1) {
            buffer.append((char) value);
        }

        return buffer.toString();
    }

    private Vector parseResponse(String text) throws LoadException {
        Vector result = new Vector();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = text.indexOf('\n', start);
            if (end == -1) {
                end = length;
            }

            String line = text.substring(start, end).trim();
            if (line.length() > 0) {
                result.addElement(parseRate(line));
            }

            start = end + 1;
        }

        if (result.size() == 0) {
            throw new LoadException(ERROR_EMPTY_RESPONSE);
        }

        return result;
    }

    private Rate parseRate(String line) throws LoadException {
        int first = line.indexOf('|');
        int second = first == -1 ? -1 : line.indexOf('|', first + 1);

        if (first <= 0 || second <= first + 1 || second >= line.length() - 1) {
            throw new LoadException(ERROR_INVALID_RESPONSE);
        }

        String code = line.substring(0, first).trim();
        String buy = line.substring(first + 1, second).trim();
        String sell = line.substring(second + 1).trim();

        if (code.length() == 0 || buy.length() == 0 || sell.length() == 0) {
            throw new LoadException(ERROR_INVALID_RESPONSE);
        }

        return new Rate(code, buy, sell);
    }

    private void showRates(Vector rates) {
        ratesList.deleteAll();

        for (int i = 0; i < rates.size(); i++) {
            Rate rate = (Rate) rates.elementAt(i);
            ratesList.append(formatRate(rate), null);
        }

        display.setCurrent(ratesList);
    }

    private String formatRate(Rate rate) {
        return rate.code + "  " + TEXT_BUY + ": " + rate.buy + "  " + TEXT_SELL + ": " + rate.sell;
    }

    private void showError(String message) {
        errorForm.deleteAll();
        if (message == null || message.length() == 0) {
            message = ERROR_UNKNOWN;
        }
        errorForm.append(message);

        Alert alert = new Alert(TITLE_ERROR, message, null, AlertType.ERROR);
        alert.setTimeout(3000);
        display.setCurrent(alert, errorForm);
    }

    private void closeInput(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeConnection(HttpConnection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static final class LoadException extends Exception {
        LoadException(String message) {
            super(message);
        }
    }

    private static final class Rate {
        final String code;
        final String buy;
        final String sell;

        Rate(String code, String buy, String sell) {
            this.code = code;
            this.buy = buy;
            this.sell = sell;
        }
    }
}
