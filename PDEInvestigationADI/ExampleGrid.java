package PDEInvestigationADI;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.GridsAndAgents.PDEGrid2D;
import HAL.Gui.GifMaker;
import HAL.Gui.GridWindow;
import HAL.Util;

import java.io.IOException;

import static HAL.Util.HeatMapBGR;
import static HAL.Util.RGB256;

class ExampleCell extends AgentSQ2Dunstackable<ExampleGrid> {
    // declare individual cell attributes
    public int type;
}

public class ExampleGrid extends AgentGrid2D<ExampleCell> {
    // declare PDE grid
    PDEGrid2D drug;
    PDEGrid2D drugA;

    public ExampleGrid(int x, int y) {
        // link agents and grid
        super(x, y, ExampleCell.class);
        // create PDE grid for drug
        drug = new PDEGrid2D(x, y);
        drugA = new PDEGrid2D(x, y);
    }

    public void initialiseDrugValues(int VESSEL, double drugConcentrationVessel) {
        // initialise drug from vessel sites
        for (ExampleCell cell:this) {
            if (cell.type == VESSEL) {
                drug.Set(cell.Xsq(), cell.Ysq(), drugConcentrationVessel);
            }
        }
        drug.Update();
    }

    public void initialiseDrugAValues(int VESSEL, double drugConcentrationVessel) {
        // initialise drug from vessel sites
        for (ExampleCell cell:this) {
            if (cell.type == VESSEL) {
                drugA.Set(cell.Xsq(), cell.Ysq(), drugConcentrationVessel);
            }
        }
        drugA.Update();
    }

    public void updatePDEValues(int VESSEL, double drugDiffusionCoefficientTimestep, double drugConcentrationVessel) {
        // diffuse drug
        drugA.Diffusion(drugDiffusionCoefficientTimestep);

        // update drug grid
        drugA.Update();
    }

    public void updatePDEADIValues(int VESSEL, double drugDiffusionCoefficientTimestep, double drugConcentrationVessel) {
        // diffuse drug
        drug.DiffusionADI(drugDiffusionCoefficientTimestep);
        // update drug grid
        drug.Update();
    }

    public double getDrugValue(int position, int iModel) {
        double drugValue;
        if (iModel == 0) {
            drugValue = drug.Get(position);
        } else {
            drugValue = drugA.Get(position);
        }
        return drugValue;
    }

    public double computeDrugValue(int iModel) {
        double nextDrugValue = getDrugValue(1, iModel);
        return nextDrugValue;
    }

    public void DrawModel(GridWindow win, int x, int y, int iModel) {
        for (int i = 0; i < length; i++) {
            // set background colour
            int color = Util.WHITE;
            if (iModel == 0) {
                // set colour for PDE
                color = HeatMapBGR(drug.Get(i) * 1E1);
            } else {
                // set colour for PDE
                color = HeatMapBGR(drugA.Get(i) * 1E1);
            }
            // set up display
            win.SetPix(i + iModel * x * y,color);
        }
    }

    public static void main(String[]args) throws IOException {
        // declare parameters
        int x = 5; // number of cells horizontal
        int y = 5; // number of cells vertical
        double timestep = 0.1; // (hours) length of timestep
        int timesteps = 100; // number of iterations of model
        int VESSEL = RGB256(128,0,0); // set colour for vessel cells
        double xDomain = 1; // (cm) horizontal length of sample space
        double yDomain = 1; // (cm) vertical length of sample space
        double deltaX = xDomain / x; // (cm) length of horizontal gridpoint
        double deltaY = yDomain / y; // (cm) length of vertical gridpoint
        double spaceConversion = (1 / deltaX) * (1 / deltaY); // space conversion parameter (used for PDEGrids)
        double drugDiffusionCoefficient = 1E-2; // (cm^2 per day) diffusion coefficient drug
        // (cm^2 per day) diffusion coefficient proliferation signal
        //double drugDiffusionCoefficientTimestep = spaceConversion * drugDiffusionCoefficientDays;
        // (cm^2 per timestep) diffusion coefficient proliferation signal
        double drugConcentrationVessel = 1; // drug concentration on delivery
        double diffCoef = drugDiffusionCoefficient * timestep / Math.pow(deltaX, 2); // non-dimensionalised diffusion coefficient
        //double diffCoef = 0.025; // non-dimensionalised diffusion coefficient set to exact value

        //System.out.println("non-dimensional diffusion coefficient: " + diffCoef);

        // set up animation
        GridWindow win = new GridWindow(x * 2, y, 80);
        ExampleGrid[] model = new ExampleGrid[2];
        for (int i = 0; i < model.length; i++) {
            model[i] = new ExampleGrid(x, y);
        }
        // declare animation as gif
        GifMaker testGif = new GifMaker("PDEInvestigation.gif", 10, false);

        for (int i = 0; i < model.length; i++) {
            // initialise vessel cells
            model[i].NewAgentSQ(12).type = VESSEL;
            if (i == 0) {
                // initialise PDEGrid values, here we have drug value of 1 at vessels sites
                model[i].initialiseDrugValues(VESSEL, drugConcentrationVessel);
            } else {
                // initialise PDEGrid values, here we have drug value of 1 at vessels sites
                model[i].initialiseDrugAValues(VESSEL, drugConcentrationVessel);
            }
            double initDrugValue = model[i].getDrugValue(1, i);
            //System.out.println("Model " + i + ", drug at site [0, 1] when t = 0: " + initDrugValue);
        }

        for (int i = 0; i < model.length; i++) {
            // draw
            model[i].DrawModel(win, x, y, i);
        }

        //model.NewAgentSQ(12).type = VESSEL;
        //model.NewAgentSQ(24).type = VESSEL;
        //model.NewAgentSQ(36).type = VESSEL;
        //model.NewAgentSQ(40).type = VESSEL;



        // main loop
        for (int j = 0; j < timesteps; j++) {
            int time = j+1;

            // time between animation frames
            win.TickPause(10);

            // test finite difference Euler scheme
            model[0].updatePDEADIValues(VESSEL, diffCoef, drugConcentrationVessel);;

            // update PDEGrid values
            model[1].updatePDEValues(VESSEL, diffCoef, drugConcentrationVessel);

            // compute error at site [0, 1] between models
            double drugValue = 0;
            double drugAValue = 0;
            for (int i = 0; i < model.length; i++) {
                if (i == 0) {
                    drugValue = model[i].computeDrugValue(i);
                } else {
                    drugAValue = model[i].computeDrugValue(i);
                }
            }
            double error = drugValue - drugAValue;
            System.out.println("Error between models at site [0, 1]: " + error);

            for (int i = 0; i < model.length; i++) {
                // increment timestep
                model[i].IncTick();

                // draw
                model[i].DrawModel(win, x, y, i);
            }


            // store frame in gif
            testGif.AddFrame(win);

        }
        // end gif storage
        testGif.Close();

        // indicate simulation has finished
        System.out.println("Simulation finished!");


    }
}
