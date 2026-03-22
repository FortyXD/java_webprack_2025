package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.Currency;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class CurrencyDaoTest extends TestBase {

    private final CurrencyDao dao = new CurrencyDao();

    @Test
    public void findAll_returnsNonEmptyList() {
        List<Currency> list = dao.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        list.forEach(c -> {
            assertNotNull(c.getId());
            assertNotNull(c.getName());
        });
    }

    @Test
    public void findById_whenExists_returnsCurrency() {
        List<Currency> all = dao.findAll();
        assertFalse(all.isEmpty());
        Optional<Currency> opt = dao.findById(all.get(0).getId());
        assertTrue(opt.isPresent());
        assertEquals(opt.get().getName(), all.get(0).getName());
    }

    @Test
    public void findById_whenNotExists_returnsEmpty() {
        assertFalse(dao.findById(999999L).isPresent());
    }

    @Test
    public void findByName_whenExists_returnsCurrency() {
        Optional<Currency> rub = dao.findByName("RUB");
        assertTrue(rub.isPresent());
        assertEquals(rub.get().getName(), "RUB");
    }

    @Test
    public void findByName_whenNotExists_returnsEmpty() {
        assertFalse(dao.findByName("XXX").isPresent());
    }
}
