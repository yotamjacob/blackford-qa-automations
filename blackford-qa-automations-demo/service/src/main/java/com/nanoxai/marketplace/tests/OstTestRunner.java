package com.nanoxai.marketplace.tests;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(Cucumber.class)
@CucumberOptions(features = "classpath:features/ost", glue = {"com.nanoxai.marketplace.tests"}, plugin = {"pretty"})
@SpringBootTest
public class OstTestRunner {
}
