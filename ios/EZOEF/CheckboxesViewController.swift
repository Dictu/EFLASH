// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class CheckboxesViewController: UIViewController {
    
    @IBOutlet var tableView: UITableView!
    
    let rowHeight: CGFloat = 44
    let headerHeight: CGFloat = 28
    let originY: CGFloat = 44

    var sectionTitles: [String] = []
    var checkedItems: [[Bool]] = []
    
    var items: [[String]] = [] {
        didSet {
            self.checkedItems = []
            for section: [String] in self.items {
                var checkedSection: [Bool] = []
                for _ in section {
                    checkedSection.append(false)
                }
                self.checkedItems.append(checkedSection)
            }
        }
    }
    
    var selectedItems: [String] {
        set {
            for (sectionIdx, section) in self.items.enumerated() {
                for (index, item) in section.enumerated() {
                    self.checkedItems[sectionIdx][index] = newValue.contains(item)
                }
            }
        }
        get {
            var selectedItems: [String] = []
            for (sectionIdx, checkedSection) in self.checkedItems.enumerated() {
                for (index, checked) in checkedSection.enumerated() {
                    if checked {
                        selectedItems.append(self.items[sectionIdx][index])
                    }
                }
            }
            return selectedItems
        }
    }
    
    var selectedIndexes: [Int] {
        set {
            var loopIndex: Int = 0
            for (sectionIdx, section) in self.items.enumerated() {
                for (index, _) in section.enumerated() {
                    self.checkedItems[sectionIdx][index] = newValue.contains(loopIndex)
                    loopIndex = loopIndex + 1
                }
            }
        }
        get {
            var selectedIndexes: [Int] = []
            var index: Int = 0
            for (_, checkedSection) in self.checkedItems.enumerated() {
                for (_, checked) in checkedSection.enumerated() {
                    if checked {
                        selectedIndexes.append(index)
                    }
                    index = index + 1
                }
            }
            return selectedIndexes
        }
    }
    
    convenience init() {
        self.init(nibName: String(describing: CheckboxesViewController.self), bundle: Bundle.main)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.tableView.tableFooterView = UIImageView()
        self.tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cellIdentifier")
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        AppAnalyticsUtil.log(event: .Scherm, segment: .CheckboxesScherm)
    }
    
    func preferredContentHeight() -> CGFloat {
        var rowCount: NSInteger = 0
        for section in self.items {
            rowCount += section.count
        }
        
        var preferredHeight: CGFloat = (CGFloat(rowCount) * self.rowHeight) + (CGFloat(self.sectionTitles.count) * self.headerHeight) + self.originY
        
        let maximumHeight: CGFloat = UIScreen.main.bounds.height - 220
        if preferredHeight > maximumHeight {
            preferredHeight = maximumHeight
        }
        return preferredHeight
    }
    
    @IBAction func selectNoneButtonPressed(_ sender: Any) {
        for (sectionIdx, checkedSection) in self.checkedItems.enumerated() {
            self.checkedItems[sectionIdx] = [Bool](repeating: false, count: checkedSection.count)
        }
        self.tableView.reloadData()
    }
    
    @IBAction func selectAllButtonPressed(_ sender: Any) {
        for (sectionIdx, checkedSection) in self.checkedItems.enumerated() {
            self.checkedItems[sectionIdx] = [Bool](repeating: true, count: checkedSection.count)
        }
        self.tableView.reloadData()
    }
}

extension CheckboxesViewController : UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        guard section < self.sectionTitles.count else {
            return nil
        }
        
        return self.sectionTitles[section]
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return self.items.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.items[section].count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cellIdentifier", for: indexPath)
        
        cell.textLabel?.text = self.items[indexPath.section][indexPath.row]
        cell.accessoryType = self.checkedItems[indexPath.section][indexPath.row] ? .checkmark : .none
        cell.backgroundColor = UIColor.clear
        
        return cell
    }
    
}

extension CheckboxesViewController : UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.tableView.deselectRow(at: indexPath, animated: true)
        
        self.tableView.beginUpdates()
        
        self.checkedItems[indexPath.section][indexPath.row] = !self.checkedItems[indexPath.section][indexPath.row]
        self.tableView.reloadRows(at: [indexPath], with: .fade)
        
        self.tableView.endUpdates()
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return self.rowHeight
    }
    
}
