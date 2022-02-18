/*
  Delete Duplicate Files #2 - Delete Duplicate Files, Compare Trusted Folder
  Written by: Keith Fenske, http://kwfenske.github.io/
  Monday, 16 November 2009
  Java class name: DeleteDupFiles2
  Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 application to compare a folder of unknown files with
  files in a known good or "trusted" folder, then delete files from the unknown
  folder that are already in the trusted folder.  This reduces the amount of
  work required to merge new files into a collection.  Unknown files are
  considered to be duplicates if they have the same size and the same MD5
  checksum.  Duplicates are not detected inside the trusted folder.  The
  probability of two files with different contents having the same size and MD5
  checksum is extremely small.

  On most systems, deleted files are permanently gone and do not appear in the
  recycle bin or trash folder.  There is no "undo" feature.  This program is
  not recommended for inexperienced users!  See the CompareFolders Java
  application for comparing two folders to determine if all files and
  subfolders are identical.  See CompareFolders or FindDupFiles to report (but
  not delete) duplicate files in arbitrary folders.  See FileChecksum to
  generate or test checksums for a single file.

  Apache License or GNU General Public License
  --------------------------------------------
  DeleteDupFiles2 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  DeleteDupFiles2  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  DeleteDupFiles2  -s  d:\fonts  d:\temp  >report.txt

  The console application will return an exit status equal to the number of
  files deleted.  The graphical interface can be very slow when the output text
  area gets too big, which will happen if thousands of files are reported.

  Restrictions and Limitations
  ----------------------------
  There are many situations where people want to delete duplicate files.  Most
  are unsafe without additional information specific to each user's needs.
  This program implements a solution where the automated deletion of files can
  usually be done without unfortunate consequences.

  Suggestions for New Features
  ----------------------------
  (1) This program does detect duplicates inside the "unknown" folder, even if
      there is no matching file in the trusted folder.  The trusted folder can
      be omitted.  Which "unknown" files are deleted depends upon the sorting
      order.  This is not properly documented as a feature.  KF, 2009-11-16.
  (2) Java will delete hidden or read-only files on Microsoft Windows and other
      systems.  The -h and -r options from the "Font Rename" application have
      been added here, then disabled.  Deleting hidden or read-only files is
      simply too dangerous without a recycle bin or trash folder.  It is much
      safer to refuse to delete these files.  KF, 2009-11-28.
  (3) Changing the preferred font for GUI items like "ToolTip.font" does not
      accomplish much.  KF, 2019-05-19.
  (4) There should be separate options for doing subfolders in the trusted and
      unknown folders.  Currently, the same option does both.  KF, 2019-05-19.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.security.*;           // MD5 and SHA1 message digests (checksums)
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class DeleteDupFiles2
{
  /* constants */

  static final long BIG_FILE_SIZE = 5 * 1024 * 1024; // "big" means over 5 MB
  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL.";
  static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss z"; // date/time format
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String DIGEST_NAME = "MD5"; // MD5 SHA-1 SHA-256 or SHA-512
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final String LICENSE_FILE = "GnuPublicLicense3.txt";
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Delete Duplicate Files, Compare Trusted Folder - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 400; // 0.400 seconds between status updates

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static boolean consoleFlag;     // true if running as a console application
  static JLabel countDialog;      // running status count of files and folders
  static JCheckBox debugCheckbox; // graphical option for <debugFlag>
  static boolean debugFlag;       // true if we show debug information
  static JCheckBox dialogAllCheckbox; // graphical option for <dialogAllFlag>
  static boolean dialogAllFlag;   // true if we apply same reply to all files
  static JLabel dialogCheckLabel, dialogDateLabel, dialogFileLabel,
    dialogPathLabel, dialogSameLabel, dialogSizeLabel; // dialog labels
  static JTextField dialogCheckText, dialogDateText, dialogFileText,
    dialogPathText, dialogSameText, dialogSizeText; // dialog text fields
  static JButton dialogDeleteButton, dialogIgnoreButton; // dialog buttons
  static boolean dialogHasReply;  // true if user gave us a reply to dialog box
  static Integer dialogLock;      // wait on this object until dialog box ends
  static boolean dialogYesDelete; // true if user wants us to delete duplicate
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static SimpleDateFormat formatDate; // formats long date/time as numeric text
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JButton licenseButton;   // "Show License" button to display GNU GPL
  static boolean licenseExists;   // true if <LICENSE_FILE> exists and is file
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JTextArea outputText;    // generated report if running as GUI
  static boolean readonlyFlag;    // true if we try to delete read-only files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static JCheckBox scrollCheckbox; // graphical option for <scrollFlag>
  static boolean scrollFlag;      // true if we scroll calls to <putOutput>
  static JButton startButton;     // "Start" button to begin file processing
  static Thread startThread;      // separate thread for doStartButton() method
  static HashMap statusMap;       // mapping between text areas and strings
  static javax.swing.Timer statusTimer; // timer for updating status message
  static long totalChkBytes, totalDelBytes, totalDupBytes, totalUnkBytes;
                                  // total number of bytes in files (size)
  static int totalChkFiles, totalDelErrors, totalDelFiles, totalDupFiles,
    totalUnkFiles, totalUnkFolders; // total number of files and folders
  static JButton trustedButton;   // button to select "trusted" file folder
  static JTextField trustedDialog; // text field for "trusted" file folder
  static File trustedFolder;      // Java object for "trusted" file folder
  static JButton unknownButton;   // button to select "unknown" file folder
  static JTextField unknownDialog; // text field for "unknown" file folder
  static File unknownFolder;      // Java object for "unknown" file folder
  static JCheckBox zeroCheckbox;  // graphical option for <zeroFlag>
  static boolean zeroFlag;        // true if we process zero-byte empty files

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    Border emptyBorder;           // remove borders around text areas
    String firstFilename, secondFilename; // parameters on the command line
    int i;                        // index variable
    Insets inputMargins;          // margins on input text areas
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    consoleFlag = false;          // assume no files or folders on command line
    debugFlag = false;            // by default, don't show debug information
    firstFilename = secondFilename = null; // parameters on the command line
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = false;           // by default, don't process hidden files
    licenseExists = (new File(LICENSE_FILE)).isFile(); // true if license file
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    readonlyFlag = false;         // by default, don't delete read-only files
    recurseFlag = true;           // by default, process subfolders
    scrollFlag = true;            // by default, scroll calls to <putOutput>
    totalChkBytes = totalDelBytes = totalDupBytes = totalUnkBytes = 0;
    totalChkFiles = totalDelErrors = totalDelFiles = totalDupFiles
      = totalUnkFiles = totalUnkFolders = 0; // no files or folders yet
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;
    zeroFlag = false;             // by default, ignore zero-byte empty files

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatDate = new SimpleDateFormat(DATE_FORMAT); // create date/time format

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file or folder name. */

    System.err.println();
    System.err.println("Two changes must be made before this program can go into general use.  One is");
    System.err.println("to call Desktop.moveToTrash() to delete files, starting with Java 9 (2017).");
    System.err.println("The other is to select which files are deleted, rather than assuming the first");
    System.err.println("file found is correct and all later files are duplicates.");
//  System.err.println();
    System.exit(0);
    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
//      || word.equals("-h") || (mswinFlag && word.equals("/h")) // see: hidden
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(0);           // exit application after printing help
      }

      else if (word.equals("-d") || (mswinFlag && word.equals("/d")))
      {
        debugFlag = true;         // show debug information
        System.err.println("main args.length = " + args.length);
        for (int k = 0; k < args.length; k ++)
          System.err.println("main args[" + k + "] = <" + args[k] + ">");
      }

//    else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
//      || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
//    {
//      hiddenFlag = true;        // process hidden files and folders
//    }
//    else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
//      hiddenFlag = false;       // ignore hidden files or subfolders

//    else if (word.equals("-r") || (mswinFlag && word.equals("/r"))
//      || word.equals("-r1") || (mswinFlag && word.equals("/r1")))
//    {
//      readonlyFlag = true;      // delete read-only files if permitted
//    }
//    else if (word.equals("-r0") || (mswinFlag && word.equals("/r0")))
//      readonlyFlag = false;     // don't try to delete read-only files

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.equals("-z") || (mswinFlag && word.equals("/z"))
        || word.equals("-z1") || (mswinFlag && word.equals("/z1")))
      {
        zeroFlag = true;          // process zero-byte empty files
      }
      else if (word.equals("-z0") || (mswinFlag && word.equals("/z0")))
        zeroFlag = false;         // ignore zero-byte empty files

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(-1);          // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume this is a file or
        folder name.  We want at most two file/folder names. */

        if (firstFilename == null) // first parameter is the trusted folder
          firstFilename = args[i];
        else if (secondFilename == null) // second parameter is unknown folder
          secondFilename = args[i];
        else
        {
          System.err.println("Too many file or folder names: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }
    }

    /* Run as a console application if files or folders were given on the
    command line.  We ignore <cancelFlag> because the user has no way of
    interrupting us at this point (no graphical interface). */

    if (firstFilename == null)    // any files or folders on command line?
    {
      /* No files or folders given.  Do nothing here.  Run as GUI later. */
    }
    else if (secondFilename == null) // only one parameter for unknown folder?
    {
      consoleFlag = true;         // don't allow GUI methods to be called
      doFileSearch(null, new File(firstFilename)); // process files and folders
      System.exit(totalDelFiles); // exit from application with status
    }
    else                          // both trusted and unknown folders given
    {
      consoleFlag = true;         // don't allow GUI methods to be called
      doFileSearch(new File(firstFilename), new File(secondFilename));
      System.exit(totalDelFiles); // exit from application with status
    }

    /* There were no file or folder names on the command line.  Open the
    graphical user interface (GUI).  We don't need to be inside an if-then-else
    construct here because the console application called System.exit() above.
    The standard Java interface style is the most reliable, but you can switch
    to something closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* We set a preferred font for GUI items that we create while building our
    layout below.  The following sets a default font for system items such as
    JFileChooser and JOptionPane dialog boxes.  There is no guarantee that the
    Java run-time environment will pay attention to these requests, or that the
    same attributes will be available in future releases of Java.  To avoid
    damaging the default look-and-feel, set only those attributes that are
    actually needed and do so before GUI items are created.  Passing a Font
    object to UIManager.put() works but can't be changed later, so pass a
    FontUIResource object or even that wrapped in UIDefaults.ProxyLazyValue.

    The following information was obtained from version 1.4.2 source code for
    javax.swing.plaf.basic.BasicLookAndFeel.java in the Sun JDK/SDK. */

    if (buttonFont != null)       // are we changing away from defaults?
    {
      javax.swing.plaf.FontUIResource uifont = new
        javax.swing.plaf.FontUIResource(buttonFont);

      UIManager.put("Button.font", uifont);
//    UIManager.put("CheckBox.font", uifont);
//    UIManager.put("CheckBoxMenuItem.acceleratorFont", uifont);
//    UIManager.put("CheckBoxMenuItem.font", uifont);
//    UIManager.put("ColorChooser.font", uifont);
      UIManager.put("ComboBox.font", uifont);
//    UIManager.put("EditorPane.font", uifont);
//    UIManager.put("FormattedTextField.font", uifont);
//    UIManager.put("InternalFrame.titleFont", uifont);
      UIManager.put("Label.font", uifont);
      UIManager.put("List.font", uifont);
//    UIManager.put("Menu.acceleratorFont", uifont);
//    UIManager.put("Menu.font", uifont);
//    UIManager.put("MenuBar.font", uifont);
//    UIManager.put("MenuItem.acceleratorFont", uifont);
//    UIManager.put("MenuItem.font", uifont);
//    UIManager.put("OptionPane.buttonFont", uifont);
//    UIManager.put("OptionPane.font", uifont);
//    UIManager.put("OptionPane.messageFont", uifont);
//    UIManager.put("Panel.font", uifont);
//    UIManager.put("PasswordField.font", uifont);
//    UIManager.put("PopupMenu.font", uifont);
//    UIManager.put("ProgressBar.font", uifont);
//    UIManager.put("RadioButton.font", uifont);
//    UIManager.put("RadioButtonMenuItem.acceleratorFont", uifont);
//    UIManager.put("RadioButtonMenuItem.font", uifont);
//    UIManager.put("ScrollPane.font", uifont);
//    UIManager.put("Spinner.font", uifont);
//    UIManager.put("TabbedPane.font", uifont);
//    UIManager.put("Table.font", uifont);
//    UIManager.put("TableHeader.font", uifont);
//    UIManager.put("TextArea.font", uifont);
      UIManager.put("TextField.font", uifont);
//    UIManager.put("TextPane.font", uifont);
//    UIManager.put("TitledBorder.font", uifont);
//    UIManager.put("ToggleButton.font", uifont);
//    UIManager.put("ToolBar.font", uifont);
      UIManager.put("ToolTip.font", uifont);
//    UIManager.put("Tree.font", uifont);
//    UIManager.put("Viewport.font", uifont);
    }

    /* Initialize shared graphical objects. */

    action = new DeleteDupFiles2User(); // create our shared action listener
    dialogLock = new Integer(0);  // wait on this object until dialog box ends
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    inputMargins = new Insets(2, 4, 2, 4); // top, left, bottom, right margins
    statusMap = new HashMap();    // mapping between text areas and strings
    statusTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update status message on clock ticks only
    trustedFolder = unknownFolder = null; // files or folders for dialog boxes

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel01, panel02, etc). */

    /* Create an outside vertical box to stack buttons/options with the inner
    dialog box for when duplicate files are found. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create an inner vertical box to stack buttons and options. */

    JPanel panel02 = new JPanel();
    panel02.setLayout(new BoxLayout(panel02, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the "trusted" folder button and matching
    dialog text. */

    JPanel panel03 = new JPanel(new GridBagLayout()); // create grid bag layout
    GridBagConstraints gbc = new GridBagConstraints(); // adjust for each field
    gbc.fill = GridBagConstraints.BOTH; // expand to available size

    gbc.gridwidth = 1;            // first column starts row
    gbc.weightx = 0.0;            // don't expand buttons
    trustedButton = new JButton("Trusted Folder...");
    trustedButton.addActionListener(action);
    if (buttonFont != null) trustedButton.setFont(buttonFont);
    trustedButton.setMnemonic(KeyEvent.VK_T);
    trustedButton.setToolTipText("Browse for \"known good\" file or folder.");
    panel03.add(trustedButton, gbc);

    panel03.add(Box.createHorizontalStrut(10), gbc); // second column (spacer)

    gbc.gridwidth = GridBagConstraints.REMAINDER; // third column ends row
    gbc.weightx = 1.0;            // give all extra space to text field
    trustedDialog = new JTextField(20);
    if (buttonFont != null) trustedDialog.setFont(buttonFont);
    trustedDialog.setMargin(inputMargins);
    trustedDialog.addActionListener(action); // do last so don't fire early
    panel03.add(trustedDialog, gbc);

    panel03.add(Box.createVerticalStrut(12), gbc); // space between panels

    /* Create a horizontal panel for the "unknown" folder button and matching
    dialog text. */

    gbc.gridwidth = 1;            // first column starts row
    gbc.weightx = 0.0;            // don't expand buttons
    unknownButton = new JButton("Unknown Folder...");
    unknownButton.addActionListener(action);
    if (buttonFont != null) unknownButton.setFont(buttonFont);
    unknownButton.setMnemonic(KeyEvent.VK_U);
    unknownButton.setToolTipText("Browse for \"unknown\" file or folder.");
    panel03.add(unknownButton, gbc);

    panel03.add(Box.createHorizontalStrut(10), gbc); // second column (spacer)

    gbc.gridwidth = GridBagConstraints.REMAINDER; // third column ends row
    gbc.weightx = 1.0;            // give all extra space to text field
    unknownDialog = new JTextField(20);
    if (buttonFont != null) unknownDialog.setFont(buttonFont);
    unknownDialog.setMargin(inputMargins);
    unknownDialog.addActionListener(action); // do last so don't fire early
    panel03.add(unknownDialog, gbc);

    panel02.add(panel03);
    panel02.add(Box.createVerticalStrut(12)); // space between panels

    /* Create a horizontal panel for the action buttons. */

    JPanel panel04 = new JPanel();
    panel04.setLayout(new BoxLayout(panel04, BoxLayout.X_AXIS));

    startButton = new JButton("Start");
    startButton.addActionListener(action);
    if (buttonFont != null) startButton.setFont(buttonFont);
    startButton.setMnemonic(KeyEvent.VK_S);
    startButton.setToolTipText("Start finding/opening files.");
    panel04.add(startButton);

    panel04.add(Box.createHorizontalGlue()); // expand horizontal space here
    panel04.add(Box.createHorizontalStrut(20)); // minimum horizontal space

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel04.add(cancelButton);

    panel04.add(Box.createHorizontalGlue());
    panel04.add(Box.createHorizontalStrut(20));

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_O);
    saveButton.setToolTipText("Copy output text to a file.");
    panel04.add(saveButton);

    panel04.add(Box.createHorizontalGlue());
    panel04.add(Box.createHorizontalStrut(20));

    licenseButton = new JButton("Show License...");
    licenseButton.addActionListener(action);
    licenseButton.setEnabled(licenseExists);
    if (buttonFont != null) licenseButton.setFont(buttonFont);
    licenseButton.setMnemonic(KeyEvent.VK_L);
    licenseButton.setToolTipText("Show GNU General Public License (GPL).");
    panel04.add(licenseButton);

    panel04.add(Box.createHorizontalGlue());
    panel04.add(Box.createHorizontalStrut(20));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel04.add(exitButton);

    panel02.add(panel04);
    panel02.add(Box.createVerticalStrut(12)); // space between panels

    /* Create a horizontal panel for the options.  An extra FlowLayout around
    the font name and size prevents those fields from expanding. */

    JPanel panel05 = new JPanel();
    panel05.setLayout(new BoxLayout(panel05, BoxLayout.X_AXIS));

    JPanel panel06 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel06.add(fontNameDialog);

    panel06.add(Box.createHorizontalStrut(5));

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    fontSizeDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for output text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel06.add(fontSizeDialog);

    panel05.add(panel06);         // add font name and size to horizontal box
    panel05.add(Box.createHorizontalGlue()); // expand horizontal space here
    panel05.add(Box.createHorizontalStrut(30)); // minimum horizontal space

    debugCheckbox = new JCheckBox("debug mode", debugFlag);
    if (buttonFont != null) debugCheckbox.setFont(buttonFont);
    debugCheckbox.setToolTipText("Verbose output, don't delete real files.");
    debugCheckbox.addActionListener(action); // do last so don't fire early
    panel05.add(debugCheckbox);

    panel05.add(Box.createHorizontalStrut(15));

    zeroCheckbox = new JCheckBox("empty files", zeroFlag);
    if (buttonFont != null) zeroCheckbox.setFont(buttonFont);
    zeroCheckbox.setToolTipText("Select to include zero-byte empty files.");
    zeroCheckbox.addActionListener(action); // do last so don't fire early
    panel05.add(zeroCheckbox);

    panel05.add(Box.createHorizontalStrut(15));

    scrollCheckbox = new JCheckBox("scroll", scrollFlag);
    if (buttonFont != null) scrollCheckbox.setFont(buttonFont);
    scrollCheckbox.setToolTipText(
      "Select to scroll displayed text, line by line.");
    scrollCheckbox.addActionListener(action); // do last so don't fire early
//  panel05.add(scrollCheckbox);

//  panel05.add(Box.createHorizontalStrut(15));

    recurseCheckbox = new JCheckBox("subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel05.add(recurseCheckbox);

    panel02.add(panel05);

    /* Put above boxed options in a panel that is centered horizontally.  Use
    FlowLayout's horizontal gap to add padding on the left and right sides. */

    JPanel panel07 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    panel07.add(panel02);
    panel01.add(panel07);
    panel01.add(Box.createVerticalStrut(30)); // space between panels

    /* Create an inner "dialog box" to be used later when a duplicate file is
    found.  GridBagLayout is the easiest way to make a regular array of field
    labels and data values, by leaving most GridBagLayout constraints at their
    default values.  These GUI fields are initially disabled. */

    JPanel panel11 = new JPanel(new GridBagLayout()); // create grid bag layout
    GridBagConstraints gbcName = new GridBagConstraints(); // for label names
    gbcName.fill = GridBagConstraints.BOTH; // expand to available size
    gbcName.gridwidth = 1;        // first column starts row
    gbcName.insets = new Insets(2, 6, 2, 6); // top, left, bottom, right
    gbcName.weightx = 0.0;        // don't expand label names
    GridBagConstraints gbcData = new GridBagConstraints(); // for field data
    gbcData.fill = GridBagConstraints.BOTH; // expand to available size
    gbcData.gridwidth = GridBagConstraints.REMAINDER; // this column ends row
    gbcData.insets = gbcName.insets; // use same margins as label names
    gbcData.weightx = 1.0;        // give all extra space to field values

    dialogFileLabel = new JLabel("File Name:", JLabel.RIGHT);
    dialogFileLabel.setEnabled(false);
    if (buttonFont != null) dialogFileLabel.setFont(buttonFont);
    panel11.add(dialogFileLabel, gbcName);
    dialogFileText = new JTextField(20);
    dialogFileText.setBorder(emptyBorder);
    dialogFileText.setEditable(false); // user can't change this text area
    dialogFileText.setEnabled(false);
    if (buttonFont != null) dialogFileText.setFont(buttonFont);
    dialogFileText.setOpaque(false); // transparent background, not white
    panel11.add(dialogFileText, gbcData);

    dialogPathLabel = new JLabel("Full Path:", JLabel.RIGHT);
    dialogPathLabel.setEnabled(false);
    if (buttonFont != null) dialogPathLabel.setFont(buttonFont);
    panel11.add(dialogPathLabel, gbcName);
    dialogPathText = new JTextField(20);
    dialogPathText.setBorder(emptyBorder);
    dialogPathText.setEditable(false); // user can't change this text area
    dialogPathText.setEnabled(false);
    if (buttonFont != null) dialogPathText.setFont(buttonFont);
    dialogPathText.setOpaque(false);
    panel11.add(dialogPathText, gbcData);

    dialogSizeLabel = new JLabel("Size (Bytes):", JLabel.RIGHT);
    dialogSizeLabel.setEnabled(false);
    if (buttonFont != null) dialogSizeLabel.setFont(buttonFont);
    panel11.add(dialogSizeLabel, gbcName);
    dialogSizeText = new JTextField(12);
    dialogSizeText.setBorder(emptyBorder);
    dialogSizeText.setEditable(false); // user can't change this text area
    dialogSizeText.setEnabled(false);
    if (buttonFont != null) dialogSizeText.setFont(buttonFont);
    dialogSizeText.setOpaque(false);
    panel11.add(dialogSizeText, gbcData);

    dialogDateLabel = new JLabel("Date, Time:", JLabel.RIGHT);
    dialogDateLabel.setEnabled(false);
    if (buttonFont != null) dialogDateLabel.setFont(buttonFont);
    panel11.add(dialogDateLabel, gbcName);
    dialogDateText = new JTextField(20);
    dialogDateText.setBorder(emptyBorder);
    dialogDateText.setEditable(false); // user can't change this text area
    dialogDateText.setEnabled(false);
    if (buttonFont != null) dialogDateText.setFont(buttonFont);
    dialogDateText.setOpaque(false);
    panel11.add(dialogDateText, gbcData);

    dialogCheckLabel = new JLabel((DIGEST_NAME + " Checksum:"), JLabel.RIGHT);
    dialogCheckLabel.setEnabled(false);
    if (buttonFont != null) dialogCheckLabel.setFont(buttonFont);
    panel11.add(dialogCheckLabel, gbcName);
    dialogCheckText = new JTextField(26);
    dialogCheckText.setBorder(emptyBorder);
    dialogCheckText.setEditable(false); // user can't change this text area
    dialogCheckText.setEnabled(false);
    if (buttonFont != null) dialogCheckText.setFont(buttonFont);
    dialogCheckText.setOpaque(false);
    panel11.add(dialogCheckText, gbcData);

    dialogSameLabel = new JLabel("Same As:", JLabel.RIGHT);
    dialogSameLabel.setEnabled(false);
    if (buttonFont != null) dialogSameLabel.setFont(buttonFont);
    panel11.add(dialogSameLabel, gbcName);
    dialogSameText = new JTextField(20);
    dialogSameText.setBorder(emptyBorder);
    dialogSameText.setEditable(false); // user can't change this text area
    dialogSameText.setEnabled(false);
    if (buttonFont != null) dialogSameText.setFont(buttonFont);
    dialogSameText.setOpaque(false);
    panel11.add(dialogSameText, gbcData);

    JPanel panel12 = new JPanel(new GridLayout(0, 1, 0, 20));
    dialogDeleteButton = new JButton("Yes, Delete File");
    dialogDeleteButton.addActionListener(action);
    dialogDeleteButton.setEnabled(false);
    if (buttonFont != null) dialogDeleteButton.setFont(buttonFont);
    dialogDeleteButton.setMnemonic(KeyEvent.VK_Y);
    dialogDeleteButton.setToolTipText("Delete this duplicate file.");
    panel12.add(dialogDeleteButton);
    dialogIgnoreButton = new JButton("Do Not Delete");
    dialogIgnoreButton.addActionListener(action);
    dialogIgnoreButton.setEnabled(false);
    if (buttonFont != null) dialogIgnoreButton.setFont(buttonFont);
    dialogIgnoreButton.setMnemonic(KeyEvent.VK_N);
    dialogIgnoreButton.setToolTipText("Keep this file, don't delete.");
    panel12.add(dialogIgnoreButton);
    dialogAllCheckbox = new JCheckBox("Do this for all files.", false);
    dialogAllCheckbox.setEnabled(false);
    if (buttonFont != null) dialogAllCheckbox.setFont(buttonFont);
    dialogAllCheckbox.setToolTipText("Select for same action all duplicates.");
    dialogAllCheckbox.addActionListener(action); // do last so don't fire early
    panel12.add(dialogAllCheckbox);

    JPanel panel13 = new JPanel(new BorderLayout(0, 0));
    panel13.add(panel12, BorderLayout.NORTH); // stop grid vertical expansion

    countDialog = new JLabel(EMPTY_STATUS, JLabel.RIGHT);
    if (buttonFont != null) countDialog.setFont(buttonFont);
    countDialog.setToolTipText("Running count of files and folders.");

    JPanel panel14 = new JPanel(new BorderLayout(20, 2));
    panel14.add(panel11, BorderLayout.CENTER); // field labels and values
    panel14.add(panel13, BorderLayout.EAST); // action buttons and options
//  panel14.add(countDialog, BorderLayout.SOUTH); // number of files, folders

    JPanel panel15 = new JPanel(new BorderLayout(0, 0));
    panel15.add(Box.createVerticalStrut(1), BorderLayout.NORTH); // top margin
    panel15.add(Box.createHorizontalStrut(4), BorderLayout.WEST); // left
    panel15.add(panel14, BorderLayout.CENTER); // actual content in center
    panel15.add(Box.createHorizontalStrut(11), BorderLayout.EAST); // right
    panel15.add(Box.createVerticalStrut(7), BorderLayout.SOUTH); // bottom
    panel15.setBorder(BorderFactory.createTitledBorder(null,
      " Duplicate File Found ", TitledBorder.DEFAULT_JUSTIFICATION,
      TitledBorder.DEFAULT_POSITION, buttonFont));

    panel01.add(panel15);
    panel01.add(Box.createVerticalStrut(25)); // space between panels

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(20, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
//  outputText.setOpaque(false);  // transparent background, not white
    outputText.setText(
        "Compare a folder of unknown files with files in a known good"
      + "\nor \"trusted\" folder, then delete files from the unknown folder"
      + "\nthat are already in the trusted folder.  Duplicate files are"
      + "\ndeleted if they have the same size and same " + DIGEST_NAME
      + " checksum."
      + "\nDuplicates are not detected inside the trusted folder.  Deleted"
      + "\nfiles are permanently gone and do not appear in the recycle"
      + "\nbin or trash folder.  There is no \"undo\" feature.  This program"
      + "\nis not recommended for inexperienced users."
      + "\n\n1. Select a \"known good\" or trusted folder."
      + "\n2. Select a folder of unknown files."
      + "\n3. Choose your options."
      + "\n4. Click the Start button.  (Click the Cancel button to stop.)"
      + "\n\nCopyright (c) 2009 by Keith Fenske.  By using this program, you"
      + "\nagree to terms and conditions of the Apache License and/or GNU"
      + "\nGeneral Public License.\n\n");

    /* Combine buttons/options with output text.  Let the text area expand and
    contract with the window size. */

    JScrollPane panel21 = new JScrollPane(outputText);
    panel21.setBorder(emptyBorder); // remove normal border from scroll pane

    JPanel panel22 = new JPanel(new BorderLayout(0, 0));
    panel22.add(panel01, BorderLayout.NORTH); // buttons and options
    panel22.add(panel21, BorderLayout.CENTER); // text area

    /* Create the main window frame for this application.  We supply our own
    margins using the edges of the frame's border layout. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel23 = mainFrame.getContentPane(); // where content meets frame
    panel23.setLayout(new BorderLayout(0, 0));
    panel23.add(Box.createVerticalStrut(12), BorderLayout.NORTH); // top margin
    panel23.add(Box.createHorizontalStrut(3), BorderLayout.WEST); // left
    panel23.add(panel22, BorderLayout.CENTER); // actual content in center
    panel23.add(Box.createHorizontalStrut(3), BorderLayout.EAST); // right
    panel23.add(Box.createVerticalStrut(3), BorderLayout.SOUTH); // bottom

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    trustedDialog.requestFocusInWindow(); // shift focus to first file name

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  calculateChecksum() method

  Given a File object, return the MD5 checksum for that file as a hexadecimal
  string.  If the checksum can not be calculated, then a string beginning with
  "unknown" is returned instead.

  Reading the input one byte at a time is a very slow way to calculate the
  checksum: about one second per megabyte on an Intel Pentium 4 at 3 GHz.
  Reading the input in a large byte buffer, and passing this buffer to the
  message digest, is over 30 times faster.
*/
  static String calculateChecksum(File givenFile)
  {
    byte[] buffer;                // input buffer for reading file
    String fileName;              // name of caller's file, without path
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    FileInputStream inStream;     // input file stream
    MessageDigest messDigest;     // object for calculating MD5 checksum
    String result;                // our result (the checksum as a string)
    long sizeDone;                // how much of <fileSize> has been finished
    long sizeUser;                // last <sizeDone> reported to user

    /* Get some initial information about the file.  If we are running as a
    graphical application, then use the inner dialog box for status. */

    fileName = givenFile.getName(); // get name of caller's file, no path
    filePath = givenFile.getPath(); // get name of caller's file, with path
    fileSize = givenFile.length(); // get size of caller's file in bytes
    sizeDone = sizeUser = 0;      // we haven't read anything yet

    if (consoleFlag == false)     // format text fields if running as GUI
    {
      /* Status messages are not synchronized until a duplicate file is found.
      They are more for the entertainment of the user than for any functional
      purpose. */

//    statusPending(dialogCheckText, EMPTY_STATUS); // will do this later
      statusPending(dialogDateText, EMPTY_STATUS); // clear date and time
      statusPending(dialogFileText, fileName); // caller's file, without path
      statusPending(dialogPathText, filePath); // caller's file, including path
      statusPending(dialogSameText, EMPTY_STATUS); // clear similar file path
//    statusPending(dialogSizeText, EMPTY_STATUS); // will do this later
    }

    try
    {
      buffer = new byte[BUFFER_SIZE]; // allocate bigger, faster input buffer
      inStream = new FileInputStream(givenFile); // open file for reading bytes
      messDigest = MessageDigest.getInstance(DIGEST_NAME);
                                  // initialize MD5 or SHA message digest
      while ((i = inStream.read(buffer, 0, BUFFER_SIZE)) > 0)
      {
        /* The user may cancel our processing if this is a very big file.  We
        must always return a String result, even when things go wrong. */

        if (cancelFlag)           // stop if user hit the panic button
        {
          inStream.close();       // try to close input file early
          return("unknown: cancelled by user for " + filePath);
        }

        /* Update the checksum calculation with the new data. */

        messDigest.update(buffer, 0, i); // update checksum with input bytes
        sizeDone += i;            // add to number of bytes finished

        /* Update the GUI status if this is a big file. */

        if ((consoleFlag == false) && ((sizeDone - sizeUser) > BIG_FILE_SIZE))
        {
          statusPending(dialogSizeText, formatComma.format(sizeDone));
                                  // count up file size until completion
          sizeUser = sizeDone;    // remember what we last told the user
        }
      }
      inStream.close();           // try to close input file
      result = formatHexBytes(messDigest.digest()); // convert to hex string
      totalChkBytes += fileSize;  // total number of bytes in checksum files
      totalChkFiles ++;           // total number of checksums calculated
      if (consoleFlag == false)   // format text fields if running as GUI
      {
        statusPending(dialogCheckText, result); // show checksum result
        statusPending(dialogSizeText, formatComma.format(sizeDone));
                                  // show file size or final completed size
      }
    }
    catch (IOException ioe)       // file may be locked, invalid, etc
    {
      result = "unknown: file I/O error for " + filePath;
    }
    catch (NoSuchAlgorithmException nsae) // report our failure as a result
    {
      result = "unknown: bad algorithm for " + filePath;
    }

    if (debugFlag)                // does user want to see what we're doing?
      putOutput(filePath + " size " + formatComma.format(fileSize)
        + " checksum " + result);
    return(result);               // return calculated MD5 checksum to caller

  } // end of calculateChecksum() method


/*
  confirmDelete() method

  Ask the user if a file should be deleted, if running as GUI.  Return <true>
  when deletion is acceptable, and <false> otherwise.
*/
  static boolean confirmDelete(
    File givenFile,               // duplicate file from "unknown" folder
    File trustFile,               // known file that has the same checksum
    String checksum)              // same MD5 checksum for both files
  {
    boolean result;               // our result (only used when running GUI)
    String tag;                   // display tag for hidden or read-only files

    if (consoleFlag)              // console mode never asks questions
      return(true);               // always allow deletion, no prompting

    /* Insert current file information into a dialog box and ask if the user
    agrees to delete the file.  We scroll the output area so that the user can
    see the most recent messages related to this file before deciding. */

    statusClear();                // cancel any pending status messages
    dialogCheckText.setText(checksum); // MD5 checksum
    dialogDateText.setText(formatDate.format(new Date(givenFile.lastModified())));
    dialogFileText.setText(givenFile.getName()); // unknown name, no path
    dialogPathText.setText(givenFile.getPath()); // unknown name with path
    dialogSameText.setText(trustFile.getPath()); // trusted name with path

    /* Use the size field for tags if this file is hidden or read-only. */

    if (givenFile.canWrite())     // is this file read-only?
    {
      if (givenFile.isHidden())   // not read-only, may be hidden
        tag = "    (hidden file)";
      else                        // not hidden, not read-only
        tag = "";
    }
    else if (givenFile.isHidden()) // read-only, is it also hidden?
      tag = "    (hidden, read-only)";
    else                          // read-only but not hidden
      tag = "    (read-only file)";
    dialogSizeText.setText(formatComma.format(givenFile.length()) + tag);

    /* The user may already have said, "Remember this answer for next time." */

    if (dialogAllFlag && dialogHasReply) // do we need to ask the question?
      return(dialogYesDelete);    // no, user has selected automatic answer

    outputText.select(999999999, 999999999); // force scroll to end of text

    /* To show the user that we are asking a question, disable most normal GUI
    fields and enable fields for the inner dialog box.  Not enabling the labels
    leaves the label text in a medium gray color for most Java look-and-feels.
    This gives some distinction between fields, without explicitly setting our
    own colors and causing problems in non-standard look-and-feels. */

    dialogAllCheckbox.setEnabled(true);
    dialogCheckLabel.setEnabled(true);
    dialogCheckText.setEnabled(true);
    dialogDateLabel.setEnabled(true);
    dialogDateText.setEnabled(true);
    dialogDeleteButton.setEnabled(true);
    dialogFileLabel.setEnabled(true);
    dialogFileText.setEnabled(true);
    dialogIgnoreButton.setEnabled(true);
    dialogIgnoreButton.requestFocusInWindow(); // shift focus to "No" button
    dialogPathLabel.setEnabled(true);
    dialogPathText.setEnabled(true);
    dialogSameLabel.setEnabled(true);
    dialogSameText.setEnabled(true);
    dialogSizeLabel.setEnabled(true);
    dialogSizeText.setEnabled(true);

    countDialog.setEnabled(false);
    debugCheckbox.setEnabled(false);
    fontNameDialog.setEnabled(false);
    fontSizeDialog.setEnabled(false);
    licenseButton.setEnabled(false);
    outputText.setEnabled(false);
    recurseCheckbox.setEnabled(false);
    saveButton.setEnabled(false);
    trustedButton.setEnabled(false);
    trustedDialog.setEnabled(false);
    unknownButton.setEnabled(false);
    unknownDialog.setEnabled(false);
    zeroCheckbox.setEnabled(false);

    statusTimer.stop();           // stop updating status on timer ticks

    /* Wait until the user gives us a reply (clicks on a button we recognize),
    or cancels the inner dialog box with the Cancel button. */

    dialogHasReply = false;       // there is no saved reply from dialog box
    dialogYesDelete = false;      // true if user wants to delete the file
    dialogWait();                 // wait for user to answer Yes, No, or Cancel

    if (dialogHasReply)           // did we receive an actual reply from user?
      result = dialogYesDelete;   // yes, then user's reply is our result
    else                          // this happens if cancelled or interrupted
      result = false;             // don't delete files unless explicit "yes"

    if (dialogAllFlag == false)   // unless user wants same answer repeated
      dialogHasReply = false;     // there is no saved reply from dialog box

    /* We are done asking a question.  Disable fields for the inner dialog box,
    and re-enable normal GUI fields. */

    dialogAllCheckbox.setEnabled(dialogAllCheckbox.isSelected());
    dialogCheckLabel.setEnabled(false);
    dialogCheckText.setEnabled(false);
    dialogDateLabel.setEnabled(false);
    dialogDateText.setEnabled(false);
    dialogDeleteButton.setEnabled(false);
    dialogFileLabel.setEnabled(false);
    dialogFileText.setEnabled(false);
    dialogIgnoreButton.setEnabled(false);
    dialogPathLabel.setEnabled(false);
    dialogPathText.setEnabled(false);
    dialogSameLabel.setEnabled(false);
    dialogSameText.setEnabled(false);
    dialogSizeLabel.setEnabled(false);
    dialogSizeText.setEnabled(false);

    countDialog.setEnabled(true);
    debugCheckbox.setEnabled(true);
    fontNameDialog.setEnabled(true);
    fontSizeDialog.setEnabled(true);
    licenseButton.setEnabled(licenseExists);
    outputText.setEnabled(true);
    recurseCheckbox.setEnabled(true);
    saveButton.setEnabled(true);
    trustedButton.setEnabled(true);
    trustedDialog.setEnabled(true);
    unknownButton.setEnabled(true);
    unknownDialog.setEnabled(true);
    zeroCheckbox.setEnabled(true);

    statusTimer.start();          // start updating status on timer ticks

    return(result);               // give caller whatever we could find

  } // end of confirmDelete() method


/*
  dialogRelease() method

  This method is called when the user clicks on a button that ends the inner
  dialog box asking whether or not to delete a duplicate file.
*/
  static void dialogRelease()
  {
    synchronized (dialogLock) { dialogLock.notify(); }
  }


/*
  dialogWait() method

  This method is called by the inner dialog box while waiting for the user to
  decide whether or not to delete a duplicate file.
*/
  static void dialogWait()
  {
    synchronized (dialogLock)
    {
      try { dialogLock.wait(); } catch (InterruptedException ie) { }
    }
  }


/*
  doCancelButton() method

  This method is called while we are opening files or folders if the user wants
  to end the processing early, perhaps because it is taking too long.  We must
  cleanly terminate any secondary threads.  Leave whatever output has already
  been generated in the output text area.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    putOutput("Cancelled by user.", true); // print message and scroll
    dialogRelease();              // release inner dialog box if we are waiting
  }


/*
  doFileSearch() method

  The caller gives us a Java File object for a "known good" (trusted) file or
  folder, and a second Java File object for an unknown file or folder.  Create
  a list of files in the trusted folder, sorted by size.  For each file in the
  unknown folder, compare MD5 checksums with trusted files of the same size.
  Delete unknown files that have matching checksums.  Add unknown files with
  unique checksums to the list, in case there are more unknown files with the
  same checksum.
*/
  static void doFileSearch(File givenTrusted, File givenUnknown)
  {
    TreeMap sizeList;             // mapping of file sizes to File objects
    File thisFile;                // current file or folder when searching
    File trusted, unknown;        // caller's File objects in canonical form

    /* Convert the caller's abstract path names to more precise canonical form,
    if the files/folders exist.  This removes differences between absolute and
    relative path names. */

    if (givenTrusted == null)     // algorithm works without pre-existing data
      trusted = null;             // so we accept missing trusted file/folder
    else if ((givenTrusted.isDirectory() == false) // if not an existing folder
      && (givenTrusted.isFile() == false)) // and if not an existing file
    {
      putOutput(("Trusted file/folder does not exist: "
        + givenTrusted.getPath()), true); // print message and scroll
      return;
    }
    else try { trusted = givenTrusted.getCanonicalFile(); }
    catch (IOException ioe)       // if the system couldn't handle file name
    {
      putOutput(("Can't convert trusted file/folder to canonical form: "
        + givenTrusted.getPath()), true);
      return;
    }

    if ((givenUnknown.isDirectory() == false) // if not an existing folder
      && (givenUnknown.isFile() == false)) // and if not an existing file
    {
      putOutput(("Unknown file/folder does not exist: "
        + givenUnknown.getPath()), true);
      return;
    }
    else try { unknown = givenUnknown.getCanonicalFile(); }
    catch (IOException ioe)       // if the system couldn't handle file name
    {
      putOutput(("Can't convert unknown file/folder to canonical form: "
        + givenUnknown.getPath()), true);
      return;
    }

    /* The trusted folder can not be the same as the unknown folder, or a file
    or subfolder anywhere inside an unknown folder.  Otherwise, all files in a
    trusted folder will be incorrectly detected as duplicates.  Be strict: our
    <recurseFlag> can change by a GUI option while we are scanning folders. */

    thisFile = trusted;           // search up directory tree starting here
    while (thisFile != null)      // ends when we hit a directory root
    {
      if (thisFile.equals(unknown)) // if same folder, or any parent is same
      {
        putOutput(
          "Trusted folder can not be same or subfolder of unknown folder.");
        putOutput("Trusted file/folder resolves to: " + trusted.getPath());
        putOutput(("Unknown file/folder resolves to: " + unknown.getPath()),
          true);                  // print message and scroll
        return;
      }
      thisFile = thisFile.getParentFile(); // go up in the directory tree
    }

    /* At this point, it looks like we will be able to proceed.  Clear the
    output text area if running as a graphical application. */

    if (consoleFlag == false)     // only if running as GUI
      outputText.setText("");     // clear output text area

    /* Call a recursive helper method to collect file names and sizes for the
    trusted folder. */

    sizeList = new TreeMap();     // start with an empty size mapping
    if (trusted != null)          // no trusted means no pre-existing file data
      doFileTrusted(sizeList, trusted, unknown); // recursive, may be cancelled

    /* Call a recursive helper method to compare file sizes and checksums for
    the unknown folder. */

    if (cancelFlag) return;       // stop if user hit the panic button
    doFileUnknown(sizeList, unknown); // recursive, may be cancelled

    /* Print a summary even if the user cancelled.  Scroll each summary line,
    because the Java 1.4 run-time may be busy displaying text and "forget" to
    scroll the last line. */

    putOutput("");                // one blank line before summary
    putOutput(("Deleted " + prettyPlural(totalDelFiles, "file") + " using "
      + prettyPlural(totalDelBytes, "byte") + ", with "
      + prettyPlural(totalDelErrors, "error") + "."), true);
    putOutput(("Found " + prettyPlural(totalDupFiles, "duplicate file")
      + " using " + prettyPlural(totalDupBytes, "byte") + "."), true);
    putOutput(("Calculated " + prettyPlural(totalChkFiles, "checksum")
      + " with " + prettyPlural(totalChkBytes, "byte") + "."), true);
    putOutput(((cancelFlag ? "Found " : "Finished ")
      + prettyPlural(totalUnkFolders, "unknown folder") + " and "
      + prettyPlural(totalUnkFiles, "file") + " using "
      + prettyPlural(totalUnkBytes, "byte") + "."), true);

  } // end of doFileSearch() method


/*
  doFileTrusted() method

  Recursively scan a file directory (folder) to create a mapping of file sizes
  to vectors of DeleteDupFiles2Data objects.  This is used for the trusted file
  or folder.  No duplicate detection occurs here.
*/
  static void doFileTrusted(
    TreeMap sizeList,             // mapping of file sizes to File objects
    File givenFile,               // caller gives us one file or folder
    File avoidFile)               // don't search this subfolder if found
  {
    File[] contents;              // contents if <givenFile> is a folder
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    File next;                    // next File object from <contents>
    Long sizeKey;                 // file size converted to an object

    if (cancelFlag) return;       // stop if user hit the panic button

    if (givenFile.equals(avoidFile)) // unknown may be subfolder of trusted
    {
      if (debugFlag)              // does user want to see what we're doing?
        putOutput(givenFile.getPath()
          + " - ignoring the \"unknown\" file/folder");
    }
    else if (givenFile.isDirectory()) // is this a folder?
    {
      if (true) // (consoleFlag || debugFlag) // trace our directory search?
        putOutput("Scanning trusted folder " + givenFile.getPath());
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          if (debugFlag)          // does user want to see what we're doing?
            putOutput(next.getPath() + " - ignoring hidden file/folder");
        }
        else if (next.isDirectory()) // is this entry for a folder?
        {
          if (recurseFlag)        // does user want us to do subfolders?
            doFileTrusted(sizeList, next, avoidFile); // yes, do recursion
          else if (debugFlag)     // does user want to see what we're doing?
            putOutput(next.getPath() + " - ignoring subfolder");
        }
        else if (next.isFile())   // entry is for a regular file
          doFileTrusted(sizeList, next, avoidFile); // always do files found
        else
          { /* Silently ignore unknown directory entries. */ }
      }
    }
    else if (givenFile.isFile())  // is this a file?
    {
      fileSize = givenFile.length(); // get size of caller's file in bytes
      if (zeroFlag || (fileSize > 0)) // normally only want non-empty files
      {
        sizeKey = new Long(fileSize); // get file size as an object
        if (sizeList.containsKey(sizeKey) == false) // map entry for this size?
          sizeList.put(sizeKey, new Vector()); // no, add empty list for size
        ((Vector) sizeList.get(sizeKey))
          .add(new DeleteDupFiles2Data(givenFile)); // append new entry
      }
      else if (debugFlag)         // does user want to see what we're doing?
        putOutput(givenFile.getPath() + " - ignoring zero-byte empty file");
    }
    else
    {
      /* Silently ignore anything we can't identify as a file or folder. */
    }
  } // end of doFileTrusted() method


/*
  doFileUnknown() method

  Recursively scan a file directory (folder) to find files that have the same
  size and checksum as files in the <sizeList> mapping.  This is used for the
  unknown file or folder.  The real duplicate detection occurs here.
*/
  static void doFileUnknown(
    TreeMap sizeList,             // mapping of file sizes to File objects
    File givenFile)               // caller gives us one file or folder
  {
    File[] contents;              // contents if <givenFile> is a folder
    String filePath;              // name of caller's file, including path
    long fileSize;                // size of caller's file in bytes
    int i;                        // index variable
    File next;                    // next File object from <contents>
    DeleteDupFiles2Data sizeEntry; // current item from <sizeVector>
    int sizeIndex;                // current index of into <sizeVector>
    Long sizeKey;                 // file size converted to an object
    int sizeLength;               // number of items in <sizeVector>
    Vector sizeVector;            // list of files having the same size
    DeleteDupFiles2Data unknownEntry; // data object for unknown file

    if (cancelFlag) return;       // stop if user hit the panic button

    filePath = givenFile.getPath(); // get name of caller's file, with path
    if (givenFile.isDirectory())  // is this a folder?
    {
      totalUnkFolders ++;         // total number of unknown folders
      if (true) // (consoleFlag || debugFlag) // trace our directory search?
        putOutput("Checking unknown folder " + givenFile.getPath());
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          if (debugFlag)          // does user want to see what we're doing?
            putOutput(next.getPath() + " - ignoring hidden file/folder");
        }
        else if (next.isDirectory()) // is this entry for a folder?
        {
          if (recurseFlag)        // does user want us to do subfolders?
            doFileUnknown(sizeList, next); // yes, do recursion
          else if (debugFlag)     // does user want to see what we're doing?
            putOutput(next.getPath() + " - ignoring subfolder");
        }
        else if (next.isFile())   // entry is for a regular file
          doFileUnknown(sizeList, next); // always do files found
        else
          { /* Silently ignore unknown directory entries. */ }
      }
    }
    else if (givenFile.isFile())  // is this a file?
    {
      fileSize = givenFile.length(); // get size of caller's file in bytes
      totalUnkBytes += fileSize;  // total number of bytes in unknown files
      totalUnkFiles ++;           // total number of unknown files
      if (zeroFlag || (fileSize > 0)) // normally only want non-empty files
      {
        sizeKey = new Long(fileSize); // get file size as an object
        if (sizeList.containsKey(sizeKey) == false) // map entry for this size?
          sizeList.put(sizeKey, new Vector()); // no, add empty list for size
        sizeVector = (Vector) sizeList.get(sizeKey); // files with same size
        sizeLength = sizeVector.size(); // get number of known files this size
        unknownEntry = new DeleteDupFiles2Data(givenFile); // saves checksum

        /* Do we need to calculate the checksum for this unknown file? */

        if (sizeLength > 0)       // don't calculate if nothing to compare to
          unknownEntry.md5 = calculateChecksum(givenFile);
        if (cancelFlag) return;   // stop if user hit the panic button

        /* Loop through all previously known files with the same size. */

        for (sizeIndex = 0; sizeIndex < sizeLength; sizeIndex ++)
        {
          sizeEntry = (DeleteDupFiles2Data) sizeVector.get(sizeIndex);

          /* Do we need to compute the checksum for this <sizeEntry>? */

          if (sizeEntry.md5 == null) // don't calculate if already done
            sizeEntry.md5 = calculateChecksum(sizeEntry.file);
          if (cancelFlag) return; // stop if user hit the panic button

          /* Does the unknown file have the same checksum as <sizeEntry>? */

          if (sizeEntry.md5.equals(unknownEntry.md5))
          {
            totalDupBytes += fileSize; // total number of duplicate bytes
            totalDupFiles ++;     // total number of duplicate files
            putOutput(filePath + " - same as " + sizeEntry.file.getPath());
            if (cancelFlag) return; // stop if user hit the panic button
            if ((readonlyFlag == false) && (givenFile.canWrite() == false))
            {
              /* On systems such as Microsoft Windows, Java can and will delete
              read-only files.  Don't allow this.  Since the read-only flag is
              generally set for a good reason, don't count this as an error. */

              putOutput(filePath + " - can't delete read-only files");
            }
            else if ((hiddenFlag == false) && givenFile.isHidden())
            {
              /* Similarly, don't delete hidden files, unless we were given
              explicit permission to do this with a command-line option. */

              putOutput(filePath + " - can't delete hidden files");
            }
            else if (confirmDelete(givenFile, sizeEntry.file, sizeEntry.md5)
              == false)           // if GUI, ask user if we can delete file
            {
              if (cancelFlag == false) // only say something if not cancelled
                putOutput(filePath + " - user said \"no\" to deletion");
            }
            else if (debugFlag)   // don't delete files while debugging
            {
              putOutput(filePath + " - debug flag simulates deletion");
            }
            else if (false && givenFile.delete()) // try to delete this file
            {
              totalDelBytes += fileSize; // total number of bytes deleted
              totalDelFiles ++;   // total number of deleted files
              putOutput(filePath + " - deleted");
            }
            else                  // Java doesn't say why delete failed
            {
              totalDelErrors ++;  // total number of failures to delete
              putOutput(filePath + " - failed to delete file");
            }
            return;               // exit early from <for> loop
          }
        }
        sizeVector.add(unknownEntry); // unknown file is unique, add to list
      }
      else if (debugFlag)         // does user want to see what we're doing?
        putOutput(filePath + " - ignoring zero-byte empty file");
    }
    else
    {
      /* Silently ignore anything we can't identify as a file or folder. */
    }
  } // end of doFileUnknown() method


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.exists() == false) // if file doesn't exist in any form
    {
      /* Maybe we can create a new file by this name.  Do nothing here.  The
      method File.canWrite() is meaningless for objects that don't exist. */
    }
    else if (userFile.isDirectory()) // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is hidden or protected.\nPlease select a normal file."));
      return;
    }
    else if (userFile.canWrite() == false) // file exists but is read-only?
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or read-only.\nCan't write to this file."));
      return;
    }
    else if (userFile.isFile() == false) // can write but isn't a file?
    {
      /* This object exists, and we can write to it, but isn't a standard file
      or folder.  Assume it is similar to the UNIX null device /dev/null and
      don't ask more questions. */
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)       // format multi-line error message
    {
      JTextArea text = new JTextArea(("Can't write to text file: "
        + ioe.getMessage()), 4, 30); // create multi-line text area
      text.setEditable(false);    // user can't change this text area
      text.setFont((Font) UIManager.get("Button.font")); // use better font
      text.setLineWrap(true);     // allow text lines to wrap
      text.setOpaque(false);      // transparent background, not white
      text.setWrapStyleWord(true); // wrap at word boundaries
      JScrollPane scroll = new JScrollPane(text); // may need scroll bars
      scroll.setBorder(BorderFactory.createEmptyBorder()); // but no borders
      JOptionPane.showMessageDialog(mainFrame, scroll); // show error message
    }
  } // end of doSaveButton() method


/*
  doStartButton() method

  The user clicked on the Start button.  First we must check that proper files
  or folders are selected for both "trusted" and "unknown".  Then we create a
  thread to run the doFileSearch() method as a background task.
*/
  static void doStartButton()
  {
    /* Do a rough check on the trusted and unknown files or folders.  We don't
    actually need a trusted file/folder for this algorithm to work; it's just a
    safer way of finding and deleting duplicate files.  We check that files or
    folders exist, even though a similar check is done by the doFileSearch()
    method, because a pop-up dialog is more normal for GUI users than an error
    message written to the bottom of the output text. */

    trustedFolder = getFileOrName(trustedFolder, trustedDialog);
    if (trustedFolder == null)    // this <if> statement can be disabled
    {
//    JOptionPane.showMessageDialog(mainFrame,
//      "Please select a trusted file or folder.");
//    return;
    }
    else if (trustedFolder.exists() == false)
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Your trusted file or folder does not exist.");
      return;
    }

    unknownFolder = getFileOrName(unknownFolder, unknownDialog);
    if (unknownFolder == null)    // an "unknown" file/folder must be given
    {
      if (trustedFolder == null)  // are both files or folders missing?
        JOptionPane.showMessageDialog(mainFrame,
          "You must select trusted and unknown folders\nbefore clicking the Start button.");
      else                        // trusted was given, unknown is missing
        JOptionPane.showMessageDialog(mainFrame,
          "Please select an unknown file or folder.");
      return;
    }
    else if (unknownFolder.exists() == false)
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Your unknown file or folder does not exist.");
      return;
    }

    /* We have our files or folders.  Disable the "Start" button until we are
    done, and enable a "Cancel" button in case our secondary thread runs for a
    long time and the user panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelButton.requestFocusInWindow(); // shift focus to "Cancel" button
    cancelFlag = false;           // but don't cancel unless user complains
    countDialog.setText(EMPTY_STATUS); // clear dialog for file counters
    dialogAllCheckbox.setEnabled(false); // force user to re-select this option
    dialogAllCheckbox.setSelected(false); // ... on each click of Start button
    dialogAllFlag = false;
    dialogCheckText.setText(EMPTY_STATUS); // clear dialog text fields
    dialogDateText.setText(EMPTY_STATUS);
    dialogFileText.setText(EMPTY_STATUS);
    dialogHasReply = false;       // there is no saved reply from dialog box
    dialogPathText.setText(EMPTY_STATUS);
    dialogSameText.setText(EMPTY_STATUS);
    dialogSizeText.setText(EMPTY_STATUS);
//  outputText.setText("");       // clear output text area
    startButton.setEnabled(false); // suspend "Start" button until we are done
    statusClear();                // cancel any pending status messages
    statusTimer.start();          // start updating status on timer ticks
    totalChkBytes = totalDelBytes = totalDupBytes = totalUnkBytes = 0;
    totalChkFiles = totalDelErrors = totalDelFiles = totalDupFiles
      = totalUnkFiles = totalUnkFolders = 0; // no files or folders yet

    startThread = new Thread(new DeleteDupFiles2User(), "doStartRunner");
    startThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    startThread.start();          // run separate thread to open files, report

  } // end of doStartButton() method


/*
  doStartRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doStartRunner()
  {
    try                           // catch most "out of memory" errors
    {
      doFileSearch(trustedFolder, unknownFolder); // process files and folders
    }
    catch (OutOfMemoryError oome) // for this thread only, not the GUI thread
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Not enough memory to complete your request.\nPlease close this program, then try increasing\nthe Java heap size with the -Xmx option on the\nJava command line.");
    }

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    dialogAllCheckbox.setEnabled(false); // force user to re-select this option
    startButton.setEnabled(true); // enable "Start" button
    startButton.requestFocusInWindow(); // shift focus to "Start" button
    statusFlush();                // force display of pending status messages
    statusTimer.stop();           // stop updating status on timer ticks

  } // end of doStartRunner() method


/*
  doTrustedButton() method

  Allow the user to select one file or folder as the "trusted" folder.  We
  don't do anything with this selection until the doStartButton() method is
  called.
*/
  static void doTrustedButton()
  {
    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Select Trusted File or Folder...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    trustedFolder = fileChooser.getSelectedFile(); // get user's selection
    trustedDialog.setText(trustedFolder.getPath()); // show name in text box
  }


/*
  doUnknownButton() method

  Allow the user to select one file or folder as the "unknown" folder.  We
  don't do anything with this selection until the doStartButton() method is
  called.
*/
  static void doUnknownButton()
  {
    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Select Unknown File or Folder...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    unknownFolder = fileChooser.getSelectedFile(); // get user's selection
    unknownDialog.setText(unknownFolder.getPath()); // show name in text box
  }


/*
  formatHexBytes() method

  Format a raw array of binary bytes as a hexadecimal string.
*/
  static String formatHexBytes(byte[] raw)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'}; // for converting binary to hexadecimal
    int i;                        // index variable
    int value;                    // one byte value from raw array

    buffer = new StringBuffer(raw.length * 2);
                                  // allocate empty string buffer for result
    for (i = 0; i < raw.length; i ++)
    {
      value = raw[i];             // get one byte value from raw array
      buffer.append(hexDigits[(value >> 4) & 0x0F]); // hex high-order nibble
      buffer.append(hexDigits[value & 0x0F]); // hex low-order nibble
    }
    return(buffer.toString());    // give caller our converted string

  } // end of formatHexBytes() method


/*
  getFileOrName() method

  The user can select a trusted or unknown folder either with a GUI button and
  its dialog box, or by typing the path name in a text field.  This is a helper
  method for doStartButton() to choose between the two, or to return <null> if
  neither is acceptable.
*/
  static File getFileOrName(File file, JTextField text)
  {
    String path;                  // text from dialog box, no trimming
    String trim;                  // text from dialog box, with trimming

    path = text.getText();        // get current text from dialog box
    if ((file != null) && file.getPath().equals(path))
      return(file);               // existing selection, no change to text

    trim = path.trim();           // remove leading and trailing spaces
    if (path.equals(trim) == false) // if trimming makes a difference
      text.setText(trim);         // then force dialog to trimmed text

    if (trim.length() > 0)        // no selection, or text has changed
      return(new File(trim));     // create a new File object from text area
    else                          // no selection, or selected path deleted
      return(null);               // no File object that we can safely use

  } // end of getFileOrName() method


/*
  prettyPlural() method

  Return a string that formats a number and appends a lowercase "s" to a word
  if the number is plural (not one).  Also provide a more general method that
  accepts both a singular word and a plural word.
*/
  static String prettyPlural(
    long number,                  // number to be formatted
    String singular)              // singular word
  {
    return(prettyPlural(number, singular, (singular + "s")));
  }

  static String prettyPlural(
    long number,                  // number to be formatted
    String singular,              // singular word
    String plural)                // plural word
  {
    final String[] names = {"zero", "one", "two"};
                                  // names for small counting numbers
    String result;                // our converted result

    if ((number >= 0) && (number < names.length))
      result = names[(int) number]; // use names for small counting numbers
    else
      result = formatComma.format(number); // format number with digit grouping

    if (number == 1)              // is the number singular or plural?
      result += " " + singular;   // append singular word
    else
      result += " " + plural;     // append plural word

    return(result);               // give caller our converted string

  } // end of prettyPlural() method


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    putOutput(text, scrollFlag);  // allow user to set default scroll behavior
  }

  static void putOutput(String text, boolean scroll)
  {
    if (consoleFlag)              // are we running as a console application?
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      if (scroll)                 // does caller want us to scroll?
        outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  DeleteDupFiles2  [options]  [trusted_folder  unknown_folder]");
    System.err.println("  java  DeleteDupFiles2  [options]  [unknown_folder]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -d = show debug information (may be verbose)");
//  System.err.println("  -h0 = ignore hidden files or folders (default)");
//  System.err.println("  -h1 = -h = process hidden files and folders");
//  System.err.println("  -r0 = don't try to delete read-only files (default)");
//  System.err.println("  -r1 = -r = delete read-only files if permitted by system");
    System.err.println("  -s0 = do only given files or folders, no subfolders");
    System.err.println("  -s1 = -s = process files, folders, and subfolders (default)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println("  -z0 = ignore zero-byte empty files (default)");
    System.err.println("  -z1 = -z = process zero-byte empty files");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.  If no file or folder names");
    System.err.println("are given on the command line, then a graphical interface will open.  PLEASE");
    System.err.println("READ THE DOCUMENTATION.  Files are deleted permanently, and do not appear in");
    System.err.println("the recycle bin or trash folder.  THERE IS NO PROMPTING WHEN RUN IN COMMAND");
    System.err.println("MODE.  If in doubt, use the -d (debug) option to disable real file deletion,");
    System.err.println("or mark files as read-only.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  sortFileList() method

  When we ask for a list of files or subfolders in a directory, the list is not
  likely to be in our preferred order.  Java does not guarantee any particular
  order, and the observed order is whatever is supplied by the underlying file
  system (which can be very jumbled for FAT16/FAT32).  We would like the file
  names to be sorted, and since we recurse on subfolders, we also want the
  subfolders to appear in order.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    String fileName;              // file name without the path
    int i;                        // index variable
    TreeMap list;                 // our list of files
    File[] result;                // our result
    StringBuffer sortKey;         // created sorting key for each file

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data.  Names are sorted as files or folders, then in lowercase
      to ignore differences in uppercase versus lowercase, then in the original
      form for systems where case is distinct. */

      list = new TreeMap();       // create empty sorted list with keys
      sortKey = new StringBuffer(); // allocate empty string buffer for keys
      for (i = 0; i < input.length; i ++)
      {
        sortKey.setLength(0);     // empty any previous contents of buffer
        if (input[i].isDirectory()) // is this "file" actually a folder?
          sortKey.append("2 ");   // yes, put subfolders after files
        else                      // must be a file or an unknown object
          sortKey.append("1 ");   // put files before subfolders

        fileName = input[i].getName(); // get the file name without the path
        sortKey.append(fileName.toLowerCase()); // start by ignoring case
        sortKey.append(" ");      // separate lowercase from original case
        sortKey.append(fileName); // then sort file name on original case
        list.put(sortKey.toString(), input[i]); // put file into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  statusClear() method

  Clear any pending status messages by setting their mapped values to <null>.
  It is more efficient to use null and non-null values than to delete then
  insert mappings into this list.
*/
  static void statusClear()
  {
    if (consoleFlag == false)     // only if running as GUI
    {
      Object[] list = statusMap.keySet().toArray(); // list of objects as keys
      for (int i = 0; i < list.length; i ++)
      {
        statusMap.put(list[i], null); // null value means nothing pending
      }
    }
  }


/*
  statusFlush() method

  Force pending status messages to be displayed in their GUI dialogs.  It is
  neither necessary nor desirable for this method to know the purpose of the
  text areas.
*/
  static void statusFlush()
  {
    String text;                  // constructed or mapped text string

    if (consoleFlag == false)     // only if running as GUI
    {
      /* For each object that maps to a non-null string, compare the string
      with the current text, and if the new string is different, then change
      the text for the object. */

      Object[] list = statusMap.keySet().toArray(); // list of objects as keys
      for (int i = 0; i < list.length; i ++)
      {
        javax.swing.text.JTextComponent field // text area as mapping index
          = (javax.swing.text.JTextComponent) list[i];
        text = (String) statusMap.get(field); // mapped string value or null
        if (text != null)         // is there a real string as a value?
        {
          String old = field.getText(); // get current value in GUI display
          if (old.equals(text) == false) // has the dialog text changed?
          {
            field.setText(text);  // yes, show the new text in GUI display
          }
          statusMap.put(field, null); // null value means nothing pending
        }
      }

      /* This program keeps counters for files and sizes.  Most are not of
      immediate interest to the user.  We show a summary in a special status
      field that only gets updated on timer intervals.  Otherwise, we would
      waste too much time formatting numbers that change very quickly. */

      if (totalUnkFiles > 0)      // have we found any files yet?
        text = "deleted: " + formatComma.format(totalDelFiles)
          + "    duplicates: " + formatComma.format(totalDupFiles)
          + "    files: " + formatComma.format(totalUnkFiles)
          + "    folders: " + formatComma.format(totalUnkFolders);
      else                        // no files, so nothing to display
        text = EMPTY_STATUS;
      if (countDialog.getText().equals(text) == false) // has text changed?
        countDialog.setText(text); // yes, replace text with new string
    }
  } // end of statusFlush() method


/*
  statusPending() method

  The caller gives a string that will be displayed in a JTextArea or JTextField
  after the next update of the status timer.  A string value of <null> means to
  do nothing, which is different than displaying the string <EMPTY_STATUS>.
*/
  static void statusPending(javax.swing.text.JTextComponent field, String text)
  {
    if (consoleFlag == false)     // only if running as GUI
    {
      statusMap.put(field, text); // any object, any value acceptable here
    }
  }


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main DeleteDupFiles2 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == debugCheckbox) // show debug information
    {
      debugFlag = debugCheckbox.isSelected();
    }
    else if (source == dialogAllCheckbox) // apply same reply to all files
    {
      dialogAllFlag = dialogAllCheckbox.isSelected();
    }
    else if (source == dialogDeleteButton) // "Yes, Delete File" button
    {
      dialogHasReply = true;      // user has answered the inner dialog box
      dialogYesDelete = true;     // and the answer is: delete the file
      dialogRelease();            // release wait on inner dialog box
    }
    else if (source == dialogIgnoreButton) // "Do Not Delete" button
    {
      dialogHasReply = true;      // user has answered the inner dialog box
      dialogYesDelete = false;    // and the answer is: don't delete file
      dialogRelease();            // release wait on inner dialog box
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == licenseButton) // "Show License" button
    {
      JTextArea text = new JTextArea(16, 48); // where to put license text
      text.setEditable(false);    // user can't change this text area
      text.setFont(new Font(SYSTEM_FONT, Font.PLAIN, 18)); // preformatted
      text.setLineWrap(false);    // don't wrap text lines
      text.setOpaque(false);      // transparent background, not white
      try { text.read(new FileReader(LICENSE_FILE), null); } // load text
      catch (IOException ioe)     // includes FileNotFoundException
      {
        JOptionPane.showMessageDialog(mainFrame,
          ("Sorry, can't read from text file:\n" + LICENSE_FILE));
        return;                   // do nothing and ignore License button
      }
      JScrollPane scroll = new JScrollPane(text); // will need scroll bars
      scroll.setBorder(BorderFactory.createEmptyBorder()); // but no borders
      JOptionPane.showMessageDialog(mainFrame, scroll,
        "GNU General Public License (GPL)", JOptionPane.PLAIN_MESSAGE);
    }
    else if (source == recurseCheckbox) // recursion for folders, subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == scrollCheckbox) // scroll calls to <putOutput>
    {
      scrollFlag = scrollCheckbox.isSelected();
    }
    else if (source == startButton) // "Start" button
    {
      doStartButton();            // start opening files or folders
    }
    else if (source == statusTimer) // update timer for status message text
    {
      statusFlush();              // force display of pending status messages
    }
    else if (source == trustedButton) // "Trusted Folder" button
    {
      doTrustedButton();          // select "trusted" file or folder
    }
    else if (source == trustedDialog) // Enter key pressed in text area
    {
      unknownDialog.requestFocusInWindow(); // makes manual data entry easier
    }
    else if (source == unknownButton) // "Unknown Folder" button
    {
      doUnknownButton();          // select "unknown" file or folder
    }
    else if (source == unknownDialog) // Enter key pressed in text area
    {
      startButton.requestFocusInWindow(); // shift focus to Start button
    }
    else if (source == zeroCheckbox) // process zero-byte empty files
    {
      zeroFlag = zeroCheckbox.isSelected();
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of DeleteDupFiles2 class

// ------------------------------------------------------------------------- //

/*
  DeleteDupFiles2Data class

  To avoid recalculating checksums, we pair Java File objects with their MD5
  checksums and use these data objects in our lists sorted by file size.
*/

class DeleteDupFiles2Data
{
  /* class variables */

  File file;                      // Java File object
  String md5;                     // MD5 checksum or <null>

  /* constructor (one argument) */

  public DeleteDupFiles2Data(File givenFile)
  {
    this.file = givenFile;        // caller must provide File value at creation
    this.md5 = null;              // checksum will be added later as necessary
  }

} // end of DeleteDupFiles2Data class

// ------------------------------------------------------------------------- //

/*
  DeleteDupFiles2User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class DeleteDupFiles2User implements ActionListener, Runnable
{
  /* empty constructor */

  public DeleteDupFiles2User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    DeleteDupFiles2.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    DeleteDupFiles2.doStartRunner();
  }

} // end of DeleteDupFiles2User class

/* Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL. */
