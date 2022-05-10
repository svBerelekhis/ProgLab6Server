package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import command.Command;
import command.CommandNames;
import object.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

/**
 * Класс для общения с базой данных и хранения вектора.
 * @autor Svetlana Berelekhis
 * @version 1.0
 */
public class Word {
    /** вектор, в котором хранятся данные*/
    Vector<Flat> flats = new Vector<>();
    /** дата создания*/
    private final Date date;
    /** set из шв, которые уже присвоены*/
    private Set<Long> ides;
    /** рандом для выбора следующего id*/
    Random random;
    /** порядок в базе*/
    boolean isAscending;
    static private String masterName;
    private Printer printer;
    private static volatile Word instance;
    private final Gson serializer = new Gson();

    /**
     * Констуктор - создание нового объекта класса
     */
    public Word(Flat[] fla, ServerWorker serverWorker){
        flats = new Vector<>();
        flats.addAll(Arrays.asList(fla));
        date = new Date();
        ides = new HashSet<>();
        random = new Random();
        isAscending = true;
        this.printer = new Printer(serverWorker);
        for(Flat flat : flats){
            ides.add(flat.getId());
        }
        sortedThis();
    }
    private Word(){
        System.out.println("Инициализация коллекции " + DatabaseManager.getInstance().getDB_URL());
        System.out.println(load());
        flats = new Vector<>();
        this.date = new Date();
        this.isAscending = true;
        load();
        sortedThis();

    }

    public void setMasterName(String masterName) {

        this.masterName = masterName;
    }

    public void setPrinter(ServerWorker serverWorker){
        this.printer = new Printer(serverWorker);
    }
    public String load() {
        try (ResultSet answer = DatabaseManager.getInstance().getConnection().createStatement().
                executeQuery("SELECT * FROM FLATS")) {
            flats.clear();
            while (answer.next()) {
                long id = answer.getLong("id");
                String name = answer.getString("name");
                Date creationDate = null;
                Coordinates coordinates = new Coordinates(answer.getLong("coordinates_x"), answer.getInt("coordinates_y"));
                try {
                    creationDate = serializer.fromJson(answer.getString("birthDate"), Date.class);
                }catch (Exception e){
                    creationDate = new Date();
                }
                Long area = answer.getLong("area");
                Long numberOfRooms = answer.getLong("numberOfRooms");
                Furnish furnish = null;
                String str = answer.getString("furnish");
                if ("DESIGNER".equals(str)){
                    furnish = Furnish.DESIGNER;
                }else if ("FINE".equals(str)){
                    furnish = Furnish.FINE;
                }else if ("BAD".equals(str)){
                    furnish = Furnish.BAD;
                }else {
                    furnish = Furnish.LITTLE;
                }
                View view = null;
                str = answer.getString("view");
                if ("PARK".equals(str)){
                    view = View.PARK;
                }else if ("GOOD".equals(str)){
                    view = View.GOOD;
                }else {
                    view = View.TERRIBLE;
                }
                Transport transport = null;
                str = answer.getString("transport");
                if("FEW".equals(str)){
                    transport = Transport.FEW;
                }else if ("NONE".equals(str)){
                    transport = Transport.NONE;
                }else if ("LITTLE".equals(str)){
                    transport = Transport.LITTLE;
                }else if ("ENOUGH".equals(str)){
                    transport = Transport.ENOUGH;
                }
                House house = new House(answer.getString("house_name"), answer.getInt("house_year"), answer.getInt("house_numberOfFloors"));
                String masterLogin = answer.getString("masterName");
                flats.add(new Flat(id, name, coordinates, creationDate, area, numberOfRooms, furnish, view, transport, house, masterLogin));
                System.out.println("flat id" + id);
            }
            return "Коллекция загружена.";
        } catch (SQLException | JsonSyntaxException e) {
            e.printStackTrace();
            return "Возникла непредвиденная ошибка. Коллекция не может быть загружена сейчас. Попробуйте позже.";
        }
    }

    public static Word getWord() {
        Word instance2 = instance;
        if (instance2 == null) {
            synchronized (Word.class) {
                instance2 = instance;
                if (instance2 == null) instance = instance2 = new Word();
            }
        } return instance;
    }
    public static Word getWord(ServerWorker serverWorker) {
        Word instance2 = instance;
        if (instance2 == null) {
            synchronized (Word.class) {
                instance2 = instance;
                if (instance2 == null) instance = instance2 = new Word();
            }
        }
        instance.setPrinter(serverWorker);
        return instance;
    }


    /**
     * Функция для передачи команды на исполнение
     * @param command - команды
     */
    public void execute(Command command){
        if (command.getCommandName() == CommandNames.HELP){
            helpCom();
        }else if (command.getCommandName() == CommandNames.INFO){
            infoCom();
        }else if (command.getCommandName() == CommandNames.SHOW){
            showCom();
        } else if (command.getCommandName() == CommandNames.CLEAR){
            clearCom();
        } else if (command.getCommandName() == CommandNames.REORDER){
            reorderCom();
        }else if (command.getCommandName() == CommandNames.PRINT_DESCENDIND){
            prDesCom();
        }else if (command.getCommandName() == CommandNames.REMOVE_BY_ID){
            try {
                try {
                    removeByIdCom(command.getId());
                } catch (ThisUserDontMasterForThisException e) {
                    printer.print("Вы не имеете прав на удаление объекта с id = " + command.getId());
                }
            } catch (IdNotFoundException e) {
                printer.print("индекс " + command.getId() + " не найден!");
            }
        }else if (command.getCommandName() == CommandNames.UPDATE_ID){
            try {
                try {
                    updateIdComm(command.getId(), command.getFlatDTO());
                } catch (ThisUserDontMasterForThisException e) {
                    printer.print("Вы не имеете прав изменение этого объекта");
                }
            } catch (IdNotFoundException e) {
                printer.print("индекс " + command.getId() + " не найден!");
            }
        }else if (command.getCommandName() == CommandNames.ADD){
            try {
                addCom(command.getFlatDTO());
            } catch (ToManyIndexesException e) {
                printer.print("вектор переполнен! Ничего не добавилось(");
            }
        } else if (command.getCommandName() == CommandNames.ADD_IF_MIN){
            try {
                addIfMINCom(command.getFlatDTO());
            } catch (ToManyIndexesException e) {
                printer.print("вектор переполнен! Ничего не добавилось(");
            }
        }else if (command.getCommandName() == CommandNames.COUNT_LESS_THEN_NUMBER_OF_ROOMS){
            countLessCom(command.getI());
        } else if (command.getCommandName() == CommandNames.REMOVE_AT){
            try {
                removeAtCom(command.getI());
            } catch (IdNotFoundException e) {
                printer.print("Такого индекса нет");
            }
        } else if (command.getCommandName() == CommandNames.FILTER_GREATER_THAN_TRANSPORT){
            filterGreaterTr(command.getTransport());
        } else if (command.getCommandName() == CommandNames.EXIT){
        }
    }

    /**
     * Функция для выполнения команды PRINT_DESCENDIND
     */
    private void prDesCom(){
        int i = flats.size() - 1;
        while (i >= 0){
            printer.print(flats.get(i).toString());
            i--;
        }
    }

    /**
     * Функция для выполнения команды REMOVE_AT
     * @param i - int, пользовательские данные
     */
    private void removeAtCom(int i) throws IdNotFoundException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "DELETE from FLATS where ID = " + i + ";";
            request.addBatch(msg);
            conn.commit();
            load();

        } catch (SQLException e) {
            throw new IdNotFoundException();
        }
    }


    public long AddFlatToBase(Flat f){
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "INSERT INTO FLATS" + f.toDBSaveForm();
            request.executeUpdate(msg, RETURN_GENERATED_KEYS);
            ResultSet rs = request.getGeneratedKeys();
            conn.commit();
            if (rs.next()) {
                return rs.getLong(2);
            }else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Функция для выполнения команды FILTER_GREATER_THAN_TRANSPORT
     * @param transport - Transport, пользовательские данные
     */
    private void filterGreaterTr(Transport transport){
        for (Flat flat : flats){
            if (flat.getTransport() != null && flat.getTransport().getCount() > transport.getCount()){
                printer.print(flat.toString());
            }
        }
    }

    /**
     * Функция для выполнения команды COUNT_LESS_THEN_NUMBER_OF_ROOMS
     * @param i - int, пользовательские данные
     */
    private void countLessCom(int i){
        int counter = 0;
        for (Flat flat : flats){
            if (flat.getNumberOfRooms() < i){
                counter += 1;
            }
        }
        printer.print(counter);
    }


    /**
     * Функция для выполнения команды HELP
     */
    private void helpCom(){
        for(CommandNames command : CommandNames.values()){
            printer.print(command.getDescription());
        }
    }

    /**
     * Функция для выполнения команды INFO
     */
    private void infoCom(){
        printer.print("Коллекция реализованная на " + flats.getClass().getName());
        printer.print("Дата создания коллекции: " + date);
        printer.print("В коллеции " + flats.size() +" элементов");
    }

    /**
     * Функция для выполнения команды SHOW
     */
    private void showCom(){
        load();
        ListOfFlat lOF = new ListOfFlat();
        for(Flat flat : flats){
            lOF.addFlat(flat);
        }
        printer.print(lOF);
    }

    /**
     * Функция для выполнения команды ADD
     * @param flatDTO - FlatDTO, пользовательские данные
     */
    private void addCom(FlatDTO flatDTO) throws ToManyIndexesException {
        addFlatToFlats(flatDTO);
        if (isAscending){
            Collections.sort(flats);
        }
        sortedThis();
    }

    /**
     * Функция для выполнения команды UPDATE_ID
     * @param id - id квартиры, в которой меняются данные
     * @param flatDTO - FlatDTO, пользовательские данные
     */
    private void updateIdComm(long id, FlatDTO flatDTO) throws IdNotFoundException, ThisUserDontMasterForThisException {
        int num = 0;
        Flat flat = flats.get(0);
        while (flat.getId() != id) {
            num += 1;
            flat = flats.get(num);
        }
        if (num < flats.size() && flats.get(num).getName().equals(this.masterName)){
            Date date = new Date();
            Flat f = new Flat(id, date, this.masterName);
            f.fromDTO(flatDTO);
            String updateTableSQL = "UPDATE FLATS SET " + f.toDBUpdateFrom() +" WHERE id = " + id + ";";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement request = conn.createStatement()) {
                conn.setAutoCommit(false);
                request.addBatch(updateTableSQL);
                request.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sortedThis();
        } else {
            throw new ThisUserDontMasterForThisException();
        }


    }


    /**
     * Функция для выполнения команды REMOVE_BY_ID
     * @param id - id квартиры, которую удаляем
     */
    private void removeByIdCom(long id) throws IdNotFoundException, ThisUserDontMasterForThisException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "DELETE FROM FLATS WHERE id = "+ id + ";";
            request.addBatch(msg);
            request.executeBatch();
            conn.commit();
            printer.print("REMOVED");

        } catch (SQLException e) {
            throw new IdNotFoundException();
        }
    }

    /**
     * Функция для выполнения команды CLEAR
     */
    private void clearCom(){
        flats = new Vector<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement request = conn.createStatement()) {
            conn.setAutoCommit(false);
            String msg = "DELETE FROM FLATS;";
            request.addBatch(msg);
            request.executeBatch();
            conn.commit();
            printer.print("YEP!!");

        } catch (SQLException e) {
            try {
                throw new IdNotFoundException();
            } catch (IdNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addFlatToFlats(FlatDTO flatDTO) {
        Date date = new Date();
        Flat flat = new Flat(0, date, this.masterName);
        flat.fromDTO(flatDTO);
        long id = AddFlatToBase(flat);
        flat.setId(id);
        flats.add(flat);
        sortedThis();
    }

    /**
     * Функция для выполнения команды ADD_IF_MIN
     * @param flatDTO - FlatDTO, пользовательские данные
     */
    private void addIfMINCom(FlatDTO flatDTO) throws ToManyIndexesException {
        if (flats.size() <= 0) {
            addCom(flatDTO);
        } else if (isAscending && flatDTO.getName().length() < flats.get(0).getName().length()){
            addCom(flatDTO);
        } else if (!isAscending && flatDTO.getName().length() < flats.get(flats.size() - 1).getName().length()){
            addCom(flatDTO);
        }
        sortedThis();
    }

    /**
     * Функция для выполнения команды REORDER
     */
    private void reorderCom(){
        isAscending = !isAscending;
        sortedThis();
    }

    /**
     * Функция, сортирующая вектор flats в зависимости от isAscending
     */
    private void sortedThis(){
        if (isAscending){
            Collections.sort(flats);
        }else {
            flats.sort(Collections.reverseOrder());
        }
    }

}
