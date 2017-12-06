package com.denimgroup.threadfix.framework.impl.django.python.runtime;

import java.util.ArrayList;
import java.util.List;

import static com.denimgroup.threadfix.CollectionUtils.list;

public abstract class PythonUnaryExpression implements PythonExpression {

    List<PythonValue> subjects = list();

    public void addSubject(PythonValue subject) {
        this.subjects.add(subject);
    }

    public void setSubjects(List<PythonValue> subjects) {
        this.subjects = new ArrayList<PythonValue>(subjects);
    }

    public int numSubjects() {
        return this.subjects.size();
    }

    public PythonValue getSubject(int idx) {
        return subjects.get(idx);
    }

    public List<PythonValue> getSubjects() {
        return subjects;
    }

    protected abstract void addPrivateSubValues(List<PythonValue> targetList);

    protected boolean replaceSubject(PythonValue oldSubject, PythonValue newSubject) {
        int idx = subjects.indexOf(oldSubject);
        if (idx < 0) {
            return false;
        }
        subjects.remove(idx);
        subjects.add(idx, newSubject);
        return true;
    }

    @Override
    public final List<PythonValue> getSubValues() {
        List<PythonValue> subValues = new ArrayList<PythonValue>(subjects);
        addPrivateSubValues(subValues);
        return subValues;
    }
}
